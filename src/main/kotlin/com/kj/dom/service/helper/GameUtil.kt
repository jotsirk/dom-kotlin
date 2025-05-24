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

object GameUtil {

    fun createAdProbabilityMap(ads: List<AdMessage>): Map<String, List<AdMessage>> {
        val easyAds = ads.filter { AdProbability.EASY.contains(AdProbability.fromDisplayName(it.probability)) }
        val mediumAds = ads.filter { AdProbability.MEDIUM.contains(AdProbability.fromDisplayName(it.probability)) }
        val hardAds = ads.filter { AdProbability.HARD.contains(AdProbability.fromDisplayName(it.probability)) }

        return mapOf(
            EASY.name to easyAds,
            MEDIUM.name to mediumAds,
            HARD.name to hardAds
        )
    }

    fun calculateAdRewardWorth(ad: AdMessage): Double {
        val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
        return (prob.successPct / 100.0) * ad.reward
    }

    fun calculateSuggestedMove(
        ads: List<AdMessage>,
        gameState: GameState,
    ): SuggestedMove {
        val groupedAdProbabilities = createAdProbabilityMap(ads)
        val worthMap: Map<AdMessage, Double> = ads.associateWith {
            calculateAdRewardWorth(
                it
            )
        }

        if (gameState.lives == 1) {
            if (gameState.gold >= HPOT.price) {
                return SuggestedMove(BUY, itemId = HPOT.id)
            } else {
                val necessaryGold = HPOT.price - gameState.gold
                val easyAds = groupedAdProbabilities[EASY.name].orEmpty()

                val suggestedEasyAdList = easyAds.filter {
                    it.reward >= necessaryGold
                }

                if (suggestedEasyAdList.size == 1) {
                    return SuggestedMove(SOLVE, adIds = listOf(suggestedEasyAdList.first().adId))
                }

                val suggestedEasyAd = easyAds
                    .filter {
                        val worth = worthMap[it] ?: 0.0
                        val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
                        worth > threshold.toDouble()
                    }
                    .maxByOrNull { it.reward }

                if (suggestedEasyAd != null) {
                    return SuggestedMove(SOLVE, adIds = listOf(suggestedEasyAd.adId))
                }

                if (ads.count { it.expiresIn == 1 } >= 3) {
                    return SuggestedMove(WAIT)
                }

                val easyAdsAccumulatedReward = easyAds
                    .filter { it.expiresIn > 1 }
                    .sumOf { it.reward }

                if (easyAdsAccumulatedReward >= necessaryGold) {
                    val validAds = easyAds
                        .filter { it.expiresIn > 1 && (it.reward) > 0 }
                        .sortedByDescending { worthMap[it] ?: 0.0 }

                    return SuggestedMove(SOLVE_MULTIPLE, adIds = validAds.map { it.adId })
                }

                val riskyAd = worthMap.maxBy { it.value }
                return SuggestedMove(SOLVE, adIds = listOf(riskyAd.key.adId))
            }
        } else {
            val acceptableAds =
                worthMap.filter { it.value >= AdProbability.fromDisplayName(it.key.probability)?.acceptableValue!! }
                    .entries.sortedByDescending { it.value }
            val easyAds = groupedAdProbabilities[EASY.name].orEmpty()

            if (acceptableAds.isEmpty()) {
                if (gameState.gold > 100 && gameState.turn < 40) {
                    SuggestedMove(BUY, ShopItemEnum.lowTierItems.random().id)
                } else if (gameState.gold > 300 && gameState.turn > 40) {
                    SuggestedMove(BUY, ShopItemEnum.highTierItems.random().id)
                }

                val acceptableEasyAds = easyAds.filter {
                    val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
                    it.reward > threshold - 3 && it.reward > HPOT.price - gameState.gold
                }.sortedByDescending { it.reward }

                val easyAd = acceptableEasyAds.firstOrNull()
                if (easyAd != null) {
                    return SuggestedMove(SOLVE, adIds = listOf(easyAd.adId))
                }

                if (easyAds.size <= 3) {
                    when {
                        gameState.gold > 100 -> return SuggestedMove(BUY, CS.id)
                        gameState.gold > 50 -> return SuggestedMove(BUY, HPOT.id)
                        else -> {
                            val fallbackAd =
                                groupedAdProbabilities[MEDIUM.name].orEmpty().firstOrNull {
                                    it.probability in listOf(
                                        QUITE_LIKELY.displayName,
                                        HMMM.displayName
                                    )
                                } ?: worthMap.maxByOrNull { it.value }?.key

                            if (fallbackAd != null) {
                                return SuggestedMove(SOLVE, adIds = listOf(fallbackAd.adId))
                            }

                            val riskyAd = worthMap.maxBy { it.value }
                            return SuggestedMove(SOLVE, adIds = listOf(riskyAd.key.adId))
                        }
                    }
                } else {
                    val easyWorthMap = worthMap.filter { it.key in easyAds }
                    val easiestAd = easyWorthMap.maxBy { it.value }
                    return SuggestedMove(SOLVE, adIds = listOf(easiestAd.key.adId))
                }
            } else {
                if (easyAds.isEmpty()) {
                    val mediumAds = groupedAdProbabilities[MEDIUM.name].orEmpty()

                    val acceptableMediumAd = mediumAds.firstOrNull {
                        val worth = worthMap[it] ?: return@firstOrNull false
                        val threshold =
                            AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: return@firstOrNull false
                        worth >= threshold
                    }

                    if (acceptableMediumAd?.probability == QUITE_LIKELY.displayName) {
                        return SuggestedMove(SOLVE, adIds = listOf(acceptableMediumAd.adId))
                    }

                    return when {
                        gameState.gold > 300 && mediumAds.size <= 2 -> SuggestedMove(BUY, MTRIX.id)
                        gameState.gold > 100 -> SuggestedMove(BUY, CS.id)
                        gameState.gold > 50 -> SuggestedMove(BUY, HPOT.id)
                        else -> {
                            val bestAd = acceptableAds.first()
                            SuggestedMove(SOLVE, adIds = listOf(bestAd.key.adId))
                        }
                    }
                } else {
                    val bestAd = acceptableAds.first()
                    return SuggestedMove(SOLVE, adIds = listOf(bestAd.key.adId))
                }
            }
        }
    }
}