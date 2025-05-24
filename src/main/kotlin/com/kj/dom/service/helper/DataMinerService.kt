package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.GameState
import com.kj.dom.model.ShopItemEnum
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.service.GameService
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataMinerService {

    @Autowired
    private lateinit var gameSerivce: GameService

    @Autowired
    private lateinit var log: Logger

    fun dataMine() {
        runRealGame()
    }

    private fun runRealGame() {
        var currentGameState = gameSerivce.startGame()

        log.info("Starting game: ${currentGameState.gameId}")

        var isGameRunning = true
        var suggestedMove: SuggestedMove? = null
        var gamesRun = 0

        while (gamesRun <= 10) {
            val adsList = gameSerivce.getAdMessages(currentGameState.gameId)

            suggestedMove = calculateRealSuggestedMove(adsList, currentGameState)

            when (suggestedMove.move) {
                "solve" -> {
                    val adId = suggestedMove.adIds.first()
                    val solveAdResponse = gameSerivce.solveAd(currentGameState.gameId, adId)
                    currentGameState = currentGameState.copy(
                        lives = solveAdResponse.lives,
                        gold = solveAdResponse.gold,
                        score = solveAdResponse.score,
                        turn = solveAdResponse.turn,
                    )
                }

                "wait" -> {
                    val shopBuyResponse = gameSerivce.buyShopItem(currentGameState.gameId, HPOT.id)
                    currentGameState = currentGameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )
                }

                "buy" -> {
                    val shopBuyResponse = gameSerivce.buyShopItem(currentGameState.gameId, suggestedMove.itemId!!)
                    currentGameState = currentGameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )
                }

                "solveMultiple" -> {
                    val adIds = suggestedMove.adIds
                    adIds.forEach {
                        val solveAdResponse = gameSerivce.solveAd(currentGameState.gameId, it)
                        currentGameState = currentGameState.copy(
                            lives = solveAdResponse.lives,
                            gold = solveAdResponse.gold,
                            score = solveAdResponse.score,
                            turn = solveAdResponse.turn,
                        )
                    }
                }
            }

            if (currentGameState.lives == 0) {
                log.info("Game over! Finished game with score ${currentGameState.score}, on turn ${currentGameState.turn}")
                gamesRun++

                currentGameState = gameSerivce.startGame()
                log.info("Starting new game: ${currentGameState.gameId}")
            }
        }
    }

    private fun calculateRealSuggestedMove(
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
                return SuggestedMove("buy", itemId = HPOT.id)
            } else {
                val necessaryGold = HPOT.price - gameState.gold
                val easyAds = groupedAdProbabilities[ProbabilityLevel.EASY.name].orEmpty()

                val suggestedEasyAdList = easyAds.filter {
                    it.reward >= necessaryGold
                }

                if (suggestedEasyAdList.size == 1) {
                    return SuggestedMove("solve", adIds = listOf(suggestedEasyAdList.first().adId))
                }

                val suggestedEasyAd = easyAds
                    .filter {
                        val worth = worthMap[it] ?: 0.0
                        val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
                        worth > threshold.toDouble()
                    }
                    .maxByOrNull { it.reward }

                if (suggestedEasyAd != null) {
                    return SuggestedMove("solve", adIds = listOf(suggestedEasyAd.adId))
                }

                if (ads.count { it.expiresIn == 1 } >= 3) {
                    return SuggestedMove("wait")
                }

                val easyAdsAccumulatedReward = easyAds
                    .filter { it.expiresIn > 1 }
                    .sumOf { it.reward }

                if (easyAdsAccumulatedReward >= necessaryGold) {
                    val validAds = easyAds
                        .filter { it.expiresIn > 1 && (it.reward) > 0 }
                        .sortedByDescending { worthMap[it] ?: 0.0 }

                    return SuggestedMove("solveMultiple", adIds = validAds.map { it.adId })
                }

                val riskyAd = worthMap.maxBy { it.value }
                return SuggestedMove("solve", adIds = listOf(riskyAd.key.adId))
            }
        } else {
            val acceptableAds =
                worthMap.filter { it.value >= AdProbability.fromDisplayName(it.key.probability)?.acceptableValue!! }
                    .entries.sortedByDescending { it.value }
            val easyAds = groupedAdProbabilities[ProbabilityLevel.EASY.name].orEmpty()

            if (acceptableAds.isEmpty()) {
                val acceptableEasyAds = easyAds.filter {
                    val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
                    val worth = worthMap[it] ?: 0.0
                    threshold > worth - 5
                }.filter {
                    it.reward > HPOT.price - gameState.gold
                }

                val easyAd = acceptableEasyAds.firstOrNull()
                if (easyAd != null) {
                    return SuggestedMove("solve", adIds = listOf(easyAd.adId))
                }

                if (easyAds.isEmpty()) {
                    when {
                        gameState.gold > 300 -> return SuggestedMove("buy", ShopItemEnum.MTRIX.id)
                        gameState.gold > 100 -> return SuggestedMove("buy", ShopItemEnum.CS.id)
                        gameState.gold > 50 -> return SuggestedMove("buy", ShopItemEnum.HPOT.id)
                        else -> {
                            val fallbackAd =
                                groupedAdProbabilities[ProbabilityLevel.MEDIUM.name].orEmpty().firstOrNull {
                                    it.probability in listOf(
                                        AdProbability.QUITE_LIKELY.displayName,
                                        AdProbability.HMMM.displayName
                                    )
                                } ?: worthMap.maxByOrNull { it.value }?.key

                            if (fallbackAd != null) {
                                return SuggestedMove("solve", adIds = listOf(fallbackAd.adId))
                            }

                            val riskyAd = worthMap.maxBy { it.value }
                            return SuggestedMove("solve", adIds = listOf(riskyAd.key.adId))
                        }
                    }
                } else {
                    val easyWorthMap = worthMap.filter { it.key in easyAds }
                    val easiestAd = easyWorthMap.maxBy { it.value }
                    return SuggestedMove("solve", adIds = listOf(easiestAd.key.adId))
                }
            } else {
                if (easyAds.isEmpty()) {
                    val mediumAds = groupedAdProbabilities[ProbabilityLevel.MEDIUM.name]

                    if (mediumAds?.isNotEmpty()!!) {
                        val acceptableMediumAd = mediumAds.firstOrNull {
                            val worth = worthMap[it] ?: return@firstOrNull false
                            val threshold =
                                AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: return@firstOrNull false
                            worth >= threshold
                        }

                        if (acceptableMediumAd != null && acceptableMediumAd.probability == AdProbability.QUITE_LIKELY.displayName) {
                            return SuggestedMove("solve", adIds = listOf(acceptableMediumAd.adId))
                        } else {
                            if (gameState.gold > 300) {
                                return SuggestedMove("buy", ShopItemEnum.MTRIX.id)
                            } else if (gameState.gold > 100 && gameState.gold < 300) {
                                return SuggestedMove("buy", ShopItemEnum.CS.id)
                            } else if (gameState.gold > 50 && gameState.gold < 100) {
                                return SuggestedMove("buy", ShopItemEnum.HPOT.id)
                            } else {
                                val bestAd = acceptableAds.first()
                                return SuggestedMove("solve", adIds = listOf(bestAd.key.adId))
                            }
                        }
                    } else {
                        if (gameState.gold > 300) {
                            return SuggestedMove("buy", ShopItemEnum.MTRIX.id)
                        } else if (gameState.gold > 100 && gameState.gold < 300) {
                            return SuggestedMove("buy", ShopItemEnum.CS.id)
                        } else if (gameState.gold > 50 && gameState.gold < 100) {
                            return SuggestedMove("buy", ShopItemEnum.HPOT.id)
                        } else {
                            val bestAd = acceptableAds.first()
                            return SuggestedMove("solve", adIds = listOf(bestAd.key.adId))
                        }
                    }
                } else {
                    val bestAd = acceptableAds.first()
                    return SuggestedMove("solve", adIds = listOf(bestAd.key.adId))
                }
            }
        }
    }

    fun calculateAdRewardWorth(ad: AdMessage): Double {
        val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
        return (prob.successPct / 100.0) * ad.reward.toInt()
    }

    private fun createAdProbabilityMap(ads: List<AdMessage>): Map<String, List<AdMessage>> {
        val easyAds = ads.filter { AdProbability.EASY.contains(AdProbability.fromDisplayName(it.probability)) }
        val mediumAds = ads.filter { AdProbability.MEDIUM.contains(AdProbability.fromDisplayName(it.probability)) }
        val hardAds = ads.filter { AdProbability.HARD.contains(AdProbability.fromDisplayName(it.probability)) }

        return mapOf(
            ProbabilityLevel.EASY.name to easyAds,
            ProbabilityLevel.MEDIUM.name to mediumAds,
            ProbabilityLevel.HARD.name to hardAds
        )
    }
}

data class AdData(
    val adCount: Int,
    val turnCount: Int,
    val ads: List<AdMessage>,
)

data class ProbabilityData(
    val runCount: Int,
    val successCount: Int,
)

data class ItemRunProbablityData(
    val item: ShopItemEnum,
    val probability: AdProbability,
    var runCount: Int,
    var successCount: Int,
)

data class ClearRunProbabilityData(
    val probability: AdProbability,
    var runCount: Int,
    var successCount: Int,
)

data class SuggestedMove(
    val move: String, // solve, wait, buy, solveMultiple
    val itemId: String? = null,
    val adIds: List<String> = emptyList(),
    val gameOverProbability: Int? = null,
)

enum class ProbabilityLevel {
    EASY,
    MEDIUM,
    HARD,
}