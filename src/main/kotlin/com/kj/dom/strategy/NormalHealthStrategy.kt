package com.kj.dom.strategy

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.AdProbability.GAMBLE
import com.kj.dom.model.AdProbability.HMMM
import com.kj.dom.model.AdProbability.QUITE_LIKELY
import com.kj.dom.model.Champion
import com.kj.dom.model.MoveType.BUY_AND_SOLVE
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel
import com.kj.dom.model.ProbabilityLevel.EASY
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
import com.kj.dom.service.helper.GameUtil

class NormalHealthStrategy : HealthStrategy {
  override fun suggestMove(
    champion: Champion,
    ads: List<AdMessage>,
  ): SuggestedMove {
    val filteredAds = ads.filterOutStealAds()
    val worthMap = filteredAds.associateWith(GameUtil::calculateAdRewardWorth)
    val grouped = GameUtil.groupAdsByProbabilityLevel(filteredAds)

    return findBestAcceptableMove(champion, grouped, worthMap)
      ?: checkExpirationFallback(filteredAds)
      ?: findMediumAdSolution(champion, grouped, worthMap, filteredAds)
  }

  private fun findBestAcceptableMove(
    champion: Champion,
    grouped: Map<ProbabilityLevel, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
  ): SuggestedMove? {
    val acceptableAds =
      worthMap
        .filter { (ad, value) ->
          value >= (AdProbability.fromDisplayName(ad.probability)?.acceptableValue?.toDouble() ?: 0.0)
        }.entries
        .sortedByDescending { it.value }
        .associate { it.toPair() }

    return findPremiumEasyAdSolution(champion, grouped[EASY].orEmpty(), acceptableAds)
      ?: acceptableAds.keys.firstOrNull()?.let {
        SuggestedMove(SOLVE, adIds = listOf(it.adId))
      }
  }

  private fun findPremiumEasyAdSolution(
    champion: Champion,
    easyAds: List<AdMessage>,
    acceptableAds: Map<AdMessage, Double>,
  ): SuggestedMove? {
    val bestAd =
      easyAds
        .filter { ad -> acceptableAds.keys.any { it.adId == ad.adId } }
        .maxByOrNull { ad -> acceptableAds[ad] ?: 0.0 }

    return bestAd?.let { ad ->
      when {
        shouldBuyItemForAd(champion, ad) -> {
          val item = ad.getBestItemForQuest(champion.gameState.turn)

          if (item != null) {
            SuggestedMove(BUY_AND_SOLVE, item.id, listOf(ad.adId))
          } else {
            SuggestedMove(SOLVE, adIds = listOf(ad.adId))
          }
        }

        else -> SuggestedMove(SOLVE, adIds = listOf(ad.adId))
      }
    }
  }

  private fun shouldBuyItemForAd(
    champion: Champion,
    ad: AdMessage,
  ): Boolean =
    ad.reward > 100 &&
      champion.gameState.gold >= CS.price &&
      ad.expiresIn > 1

  private fun checkExpirationFallback(ads: List<AdMessage>): SuggestedMove? =
    ads
      .takeIf { it.count { ad -> ad.expiresIn == 1 } >= 2 }
      ?.let { SuggestedMove(WAIT) }

  private fun findMediumAdSolution(
    champion: Champion,
    grouped: Map<ProbabilityLevel, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
  ): SuggestedMove {
    val mediumAds = grouped[MEDIUM].orEmpty()
    val fallbackAd =
      mediumAds
        .filter {
          it.probability in
            setOf(
              QUITE_LIKELY.displayName,
              HMMM.displayName,
              GAMBLE.displayName,
            ) &&
            it.reward > 50
        }.maxByOrNull { worthMap[it] ?: 0.0 }
        ?: ads.highestSuccessAdWithHighestReward()

    return createFallbackMove(champion, fallbackAd)
  }

  private fun createFallbackMove(
    champion: Champion,
    ad: AdMessage,
  ): SuggestedMove =
    if (ad.reward > 75 && ad.expiresIn > 1 && champion.gameState.gold >= 100) {
      val item = ad.getBestItemForQuest(champion.gameState.turn)
      if (item != null) {
        SuggestedMove(BUY_AND_SOLVE, item.id, listOf(ad.adId))
      } else {
        SuggestedMove(SOLVE, adIds = listOf(ad.adId))
      }
    } else {
      SuggestedMove(SOLVE, adIds = listOf(ad.adId))
    }

  private fun AdMessage.getBestItemForQuest(turnCount: Int): ShopItemEnum? =
    when {
      this.message.contains(AD_MESSAGE_DEFEND, true) -> if (turnCount < 40) WINGPOT else WINGPOTMAX
      this.message.contains(AD_MESSAGE_RESCUE, true) -> if (turnCount < 40) WINGPOT else WINGPOTMAX
      this.message.contains(AD_MESSAGE_ESCORT, true) -> if (turnCount < 40) GAS else RF
      this.message.contains(AD_MESSAGE_INVESTIGATE, true) -> if (turnCount < 40) WAX else IRON
      this.message.contains(AD_MESSAGE_INFILTRATE, true) -> if (turnCount < 40) TRICKS else MTRIX
      else -> HPOT
    }

  private fun List<AdMessage>.highestSuccessAdWithHighestReward(): AdMessage =
    this.maxWith(
      compareBy<AdMessage> {
        AdProbability.fromDisplayName(it.probability)?.successPct ?: 0
      }.thenBy { it.reward },
    )

  private companion object {
    const val AD_MESSAGE_DEFEND = "defend"
    const val AD_MESSAGE_RESCUE = "rescue"
    const val AD_MESSAGE_ESCORT = "escort"
    const val AD_MESSAGE_INVESTIGATE = "investigate"
    const val AD_MESSAGE_INFILTRATE = "infiltrate"
  }
}
