package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.AdProbability.HMMM
import com.kj.dom.model.AdProbability.QUITE_LIKELY
import com.kj.dom.model.GameState
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.HARD
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.ShopItemEnum
import com.kj.dom.model.ShopItemEnum.CS
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.ShopItemEnum.MTRIX
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
    gameState: GameState,
  ): SuggestedMove {
    val groupedAdProbabilities = createAdProbabilityMap(ads)
    val worthMap = ads.associateWith(::calculateAdRewardWorth)

    return when {
      gameState.lives == 1 -> handleLowHealth(gameState, groupedAdProbabilities, worthMap, ads)
      else -> handleNormal(gameState, groupedAdProbabilities, worthMap, ads)
    }
  }

  private fun handleLowHealth(
    gameState: GameState,
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
  ): SuggestedMove {
    if (gameState.gold >= HPOT.price) return SuggestedMove(BUY, itemId = HPOT.id)

    val easyAds = grouped[EASY.name].orEmpty()
    val neededGold = HPOT.price - gameState.gold

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

    if (ads.count { it.expiresIn == 1 } >= 3) return SuggestedMove(WAIT)

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
    gameState: GameState,
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
    ads: List<AdMessage>,
  ): SuggestedMove {
    val acceptableAds =
      worthMap
        .filter { (ad, value) ->
          value >= (AdProbability.fromDisplayName(ad.probability)?.acceptableValue?.toDouble() ?: 0.0)
        }.entries
        .sortedByDescending { it.value }

    val easyAds = grouped[EASY.name].orEmpty()

    if (acceptableAds.isEmpty()) {
      if (gameState.gold > 100 &&
        gameState.turn < 40
      ) {
        return SuggestedMove(BUY, ShopItemEnum.lowTierItems.random().id)
      }
      if (gameState.gold > 300 &&
        gameState.turn > 40
      ) {
        return SuggestedMove(BUY, ShopItemEnum.highTierItems.random().id)
      }

      easyAds
        .filter {
          val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
          it.reward > threshold - 3 && it.reward > HPOT.price - gameState.gold
        }.maxByOrNull { it.reward }
        ?.let { return SuggestedMove(SOLVE, adIds = listOf(it.adId)) }

      if (easyAds.size <= 3) {
        return when {
          gameState.gold > 100 -> SuggestedMove(BUY, CS.id)
          gameState.gold > 50 -> SuggestedMove(BUY, HPOT.id)
          else -> fallbackSolve(grouped, worthMap)
        }
      }

      val easyWorth = worthMap.filterKeys { it in easyAds }
      val easiest = easyWorth.maxByOrNull { it.value }?.key ?: return fallbackSolve(grouped, worthMap)
      return SuggestedMove(SOLVE, adIds = listOf(easiest.adId))
    }

    if (easyAds.isEmpty()) {
      val mediumAds = grouped[MEDIUM.name].orEmpty()
      val acceptableMedium =
        mediumAds.firstOrNull {
          val worth = worthMap[it] ?: return@firstOrNull false
          val threshold =
            AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: return@firstOrNull false
          worth >= threshold
        }

      if (acceptableMedium?.probability == QUITE_LIKELY.displayName) {
        return SuggestedMove(SOLVE, adIds = listOf(acceptableMedium.adId))
      }

      return when {
        gameState.gold > 300 && mediumAds.size <= 2 -> SuggestedMove(BUY, MTRIX.id)
        gameState.gold > 100 -> SuggestedMove(BUY, CS.id)
        gameState.gold > 50 -> SuggestedMove(BUY, HPOT.id)
        else -> SuggestedMove(SOLVE, adIds = listOf(acceptableAds.first().key.adId))
      }
    }

    return SuggestedMove(SOLVE, adIds = listOf(acceptableAds.first().key.adId))
  }

  private fun fallbackSolve(
    grouped: Map<String, List<AdMessage>>,
    worthMap: Map<AdMessage, Double>,
  ): SuggestedMove {
    val fallback =
      grouped[MEDIUM.name].orEmpty().firstOrNull {
        it.probability in listOf(QUITE_LIKELY.displayName, HMMM.displayName)
      } ?: worthMap.maxByOrNull { it.value }?.key

    return SuggestedMove(SOLVE, adIds = listOfNotNull(fallback?.adId))
  }
}
