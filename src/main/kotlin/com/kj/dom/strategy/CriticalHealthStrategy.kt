package com.kj.dom.strategy

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.Champion
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.SuggestedMove
import com.kj.dom.service.helper.GameUtil

class CriticalHealthStrategy : HealthStrategy {
  override fun suggestMove(
    champion: Champion,
    ads: List<AdMessage>,
  ): SuggestedMove {
    val filteredAds = ads.filterOutStealAds()
    val neededGold = HPOT.price - champion.gameState.gold
    val easyAds = GameUtil.groupAdsByProbabilityLevel(filteredAds)[EASY].orEmpty()
    val worthMap = filteredAds.associateWith { GameUtil.calculateAdRewardWorth(it) }

    return findImmediateHealthPotionSolution(champion)
      ?: findSingleAdSolution(neededGold, easyAds)
      ?: findValuableAdSolution(easyAds, worthMap)
      ?: decideWaitOrMultiAdSolution(ads, easyAds, worthMap, neededGold)
      ?: createFallbackSolution(worthMap)
  }

  private fun findImmediateHealthPotionSolution(champion: Champion): SuggestedMove? =
    champion
      .takeIf { it.canAffordHealthPotion() }
      ?.let { SuggestedMove(BUY, itemId = HPOT.id) }

  private fun findSingleAdSolution(
    neededGold: Int,
    easyAds: List<AdMessage>,
  ): SuggestedMove? =
    easyAds
      .firstOrNull { it.reward >= neededGold }
      ?.let { SuggestedMove(SOLVE, adIds = listOf(it.adId)) }

  private fun findValuableAdSolution(
    easyAds: List<AdMessage>,
    worthMap: Map<AdMessage, Double>,
  ): SuggestedMove? = easyAds
    .filter {
      (worthMap[it] ?: 0.0) > (AdProbability.fromDisplayName(it.probability)?.acceptableValue?.toDouble() ?: 0.0)
    }.maxByOrNull { worthMap[it] ?: 0.0 }
    ?.let { SuggestedMove(SOLVE, adIds = listOf(it.adId)) }

  private fun decideWaitOrMultiAdSolution(
    ads: List<AdMessage>,
    easyAds: List<AdMessage>,
    worthMap: Map<AdMessage, Double>,
    neededGold: Int,
  ): SuggestedMove? {
    val valuableAds =
      easyAds
        .filter {
          val acceptableValue =
            AdProbability
              .fromDisplayName(it.probability)
              ?.acceptableValue
              ?.toDouble()
              ?: 0.0
          (worthMap[it] ?: 0.0) > acceptableValue && it.reward > 0
        }.sortedByDescending { worthMap[it] ?: 0.0 }

    if (ads.count { it.expiresIn == 1 } >= 2) {
      return SuggestedMove(WAIT)
    }

    return valuableAds
      .takeIf { valid ->
        valid.sumOf { it.reward } >= neededGold &&
          valid.all { it.expiresIn > 1 }
      }?.let { ad -> SuggestedMove(SOLVE_MULTIPLE, adIds = ad.map { it.adId }) }
  }

  private fun createFallbackSolution(worthMap: Map<AdMessage, Double>) =
    worthMap
      .maxBy { it.value }
      .key
      .let { SuggestedMove(SOLVE, adIds = listOf(it.adId)) }
}
