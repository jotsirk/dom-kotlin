package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.AdProbability.GAMBLE
import com.kj.dom.model.AdProbability.HMMM
import com.kj.dom.model.AdProbability.QUITE_LIKELY
import com.kj.dom.model.Champion
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.BUY_AND_SOLVE
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.HARD
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.ShopItemEnum
import com.kj.dom.model.ShopItemEnum.CS
import com.kj.dom.model.ShopItemEnum.GAS
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.ShopItemEnum.IRON
import com.kj.dom.model.ShopItemEnum.MTRIX
import com.kj.dom.model.ShopItemEnum.RF
import com.kj.dom.model.ShopItemEnum.TRICKS
import com.kj.dom.model.ShopItemEnum.WAX
import com.kj.dom.model.ShopItemEnum.WINGPOT
import com.kj.dom.model.ShopItemEnum.WINGPOTMAX
import com.kj.dom.model.SuggestedMove
import java.util.Base64

object GameUtil {
  fun createAdProbabilityMap(ads: List<AdMessage>): Map<String, List<AdMessage>> {
    val easyAds = ads.filter { AdProbability.EASY.contains(AdProbability.fromDisplayName(it.probability)) }
    val mediumAds = ads.filter { AdProbability.MEDIUM.contains(AdProbability.fromDisplayName(it.probability)) }
    val hardAds = ads.filter { AdProbability.HARD.contains(AdProbability.fromDisplayName(it.probability)) }

    return mapOf(
      EASY.name to easyAds,
      MEDIUM.name to mediumAds,
      HARD.name to hardAds,
    )
  }

  fun calculateAdRewardWorth(ad: AdMessage): Double {
    val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
    return (prob.successPct / 100.0) * ad.reward
  }

  fun decodeBase64(encoded: String): String {
    return String(Base64.getDecoder().decode(encoded))
  }

  fun calculateSuggestedMove(
    ads: List<AdMessage>,
    champion: Champion,
  ): SuggestedMove {
    val filteredAds = ads.filter { !it.message.contains("steal", true) }
    val groupedAdProbabilities = createAdProbabilityMap(filteredAds)
    val worthMap = filteredAds.associateWith(::calculateAdRewardWorth)

    return when {
      champion.gameState.lives == 1 -> handleLowHealth(champion, groupedAdProbabilities, worthMap, filteredAds)
      else -> handleNormal(champion, groupedAdProbabilities, worthMap, filteredAds)
    }
  }

  private fun handleLowHealth(
    champion: Champion,
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
  ): SuggestedMove {
    if (champion.gameState.gold >= HPOT.price) return SuggestedMove(BUY, itemId = HPOT.id)

    val easyAds = grouped[EASY.name].orEmpty()
    val neededGold = HPOT.price - champion.gameState.gold

    easyAds.singleOrNull { it.reward >= neededGold }?.let {
      return SuggestedMove(SOLVE, adIds = listOf(it.adId))
    }

    easyAds
      .filter {
        (worthMap[it] ?: 0.0) > (
          AdProbability.fromDisplayName(it.probability)?.acceptableValue?.toDouble()
            ?: 0.0
        )
      }.maxByOrNull { it.reward }
      ?.let { return SuggestedMove(SOLVE, adIds = listOf(it.adId)) }

    if (ads.count { it.expiresIn == 1 } >= 2) return SuggestedMove(WAIT)

    val validAds =
      easyAds
        .filter { it.expiresIn > 1 && it.reward > 0 }
        .sortedByDescending { worthMap[it] ?: 0.0 }

    if (validAds.sumOf { it.reward } >= neededGold) {
      return SuggestedMove(SOLVE_MULTIPLE, adIds = validAds.map { it.adId })
    }

    return SuggestedMove(SOLVE, adIds = listOf(worthMap.maxBy { it.value }.key.adId))
  }

  private fun handleNormal(
    champion: Champion,
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
  ): SuggestedMove {
    val acceptableAds: Map<AdMessage, Double> =
      worthMap
        .filter { (ad, value) ->
          value >= (AdProbability.fromDisplayName(ad.probability)?.acceptableValue?.toDouble() ?: 0.0)
        }.toList()
        .sortedByDescending { it.second }
        .toMap()
    val easyAds = grouped[EASY.name].orEmpty()

    findBestAcceptableEasyAd(acceptableAds, easyAds, champion)?.let { return it }

    if (acceptableAds.isEmpty()) {
      if (ads.count { it.expiresIn == 1 } >= 2) return SuggestedMove(WAIT)
    }

    return fallbackSolve(grouped, worthMap, ads, champion)
  }

  private fun findBestAcceptableEasyAd(
    acceptableAds: Map<AdMessage, Double>,
    easyAds: List<AdMessage>,
    champion: Champion,
  ): SuggestedMove? {
    val acceptableAdSet = acceptableAds.keys.toSet()
    val bestAd =
      easyAds
        .asSequence()
        .filter { ad -> acceptableAdSet.any { it.adId == ad.adId } }
        .maxByOrNull { ad -> acceptableAds.entries.find { it.key.adId == ad.adId }?.value ?: 0.0 }

    bestAd?.let {
      val canAffordLowTier = champion.gameState.gold >= CS.price
      val hasHighReward = it.reward > 100
      val notExpiringSoon = it.expiresIn > 1

      if (hasHighReward && canAffordLowTier && notExpiringSoon) {
        val bestItemForQuest = bestAd.getBestItemForQuest(champion.gameState.turn)

        return if (bestItemForQuest != null) {
          SuggestedMove(
            BUY_AND_SOLVE,
            itemId = bestItemForQuest.id,
            adIds = listOf(it.adId),
          )
        } else {
          SuggestedMove(SOLVE, adIds = listOf(it.adId))
        }
      }

      return SuggestedMove(SOLVE, adIds = listOf(it.adId))
    }

    return null
  }

  private fun fallbackSolve(
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
    champion: Champion,
  ): SuggestedMove {
    val mediumAds = grouped[MEDIUM.name].orEmpty()

    var fallbackAd =
      mediumAds
        .filter {
          it.probability in
            listOf(
              QUITE_LIKELY.displayName,
              HMMM.displayName,
              GAMBLE.displayName,
            ) &&
            it.reward > 50
        }.maxByOrNull { worthMap[it] ?: 0.0 }

    if (fallbackAd == null) {
      fallbackAd = ads.highestSuccessAdWithHighestReward()
    } else {
      if (fallbackAd.reward > 75 && fallbackAd.expiresIn > 1 && champion.gameState.gold >= 100) {
        val bestItem = fallbackAd.getBestItemForQuest(champion.gameState.turn)
        return if (bestItem != null) {
          SuggestedMove(
            BUY_AND_SOLVE,
            itemId = bestItem.id,
            adIds = listOf(fallbackAd.adId),
          )
        } else {
          SuggestedMove(SOLVE, adIds = listOf(fallbackAd.adId))
        }
      }
    }

    return SuggestedMove(SOLVE, adIds = listOf(fallbackAd.adId))
  }

  private fun AdMessage.getBestItemForQuest(turnCount: Int): ShopItemEnum? =
    when {
      this.message.contains("defend", true) -> if (turnCount < 40) WINGPOT else WINGPOTMAX
      this.message.contains("rescue", true) -> if (turnCount < 40) WINGPOT else WINGPOTMAX
      this.message.contains("escort", true) -> if (turnCount < 40) GAS else RF
      this.message.contains("investigate", true) -> if (turnCount < 40) WAX else IRON
      this.message.contains("infiltrate", true) -> if (turnCount < 40) TRICKS else MTRIX
      else -> HPOT
    }

  private fun List<AdMessage>.highestSuccessAdWithHighestReward(): AdMessage =
    this.maxWith(
      compareBy<AdMessage> {
        AdProbability.fromDisplayName(it.probability)?.successPct ?: 0
      }.thenBy { it.reward }
    )
}
