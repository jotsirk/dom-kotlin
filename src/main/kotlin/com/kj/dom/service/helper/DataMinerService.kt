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
        gameSerivce.startGame()
        var currentGameState = gameSerivce.getGameState()

        log.info("Starting game: ${currentGameState.gameId}")

        var isGameRunning = true
        var suggestedMove: SuggestedMove? = null

        while (isGameRunning) {
            val adsList = gameSerivce.getAdMessages(currentGameState.gameId)

            suggestedMove = calculateRealSuggestedMove(adsList, currentGameState, suggestedMove?.move == "wait")

            if (suggestedMove == null) {
                log.info("something impossible happened, finishing game")
                log.info("Finished game with score ${currentGameState.score}, on turn ${currentGameState.turn}")
                isGameRunning = false
                continue
            }

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
                isGameRunning = false
            }
        }
    }

    private fun calculateRealSuggestedMove(
        ads: List<AdMessage>,
        gameState: GameState,
        skippedTurn: Boolean = false,
    ): SuggestedMove? {
        val groupedAdProbabilities = createAdProbabilityMap(ads)
        val worthMap: Map<AdMessage, Double> = ads.associateWith {
            calculateAdRewardWorth(
                it
            )
        }

        if (gameState.lives == 1 && gameState.gold >= HPOT.price) {
            return SuggestedMove("buy", itemId = HPOT.id)
        } else if (gameState.lives == 1 && gameState.gold < HPOT.price) {
            if (groupedAdProbabilities[ProbabilityLevel.EASY.name]?.isEmpty() == true) {
                if (ads.any { it.expiresIn == 1 } && !skippedTurn) {
                    // TODO add an already skipped possibility too to continue and not get stuck in a loop
                    return SuggestedMove("wait")
                }
            } else {
                val acceptableAds = worthMap
                    .filter { it.value >= AdProbability.fromDisplayName(it.key.probability)?.acceptableValue!! }
                    .entries.sortedByDescending { it.value }

                if (acceptableAds.isEmpty()) {
                    val bestPossibleAd = worthMap.maxBy { it.value }
                    return SuggestedMove("solve", adIds = listOf(bestPossibleAd.key.adId))
                }

                val necessaryGold = HPOT.price - gameState.gold
                val bestAd = acceptableAds.first()

                if (bestAd.key.reward.toInt() > necessaryGold) {
                    return SuggestedMove("solve", adIds = listOf(bestAd.key.adId))
                }

                if (acceptableAds.size == 1) {
                    val bestAd1 = acceptableAds.first()
                    return SuggestedMove("solve", adIds = listOf(bestAd1.key.adId))
                } else {
                    val bestAd2 = acceptableAds.firstOrNull { it.key.expiresIn != 1 }
                        ?: worthMap.maxBy { it.value }
                    return SuggestedMove("solve", adIds = listOf(bestAd2.key.adId))
                }
            }
        } else {
            val acceptableAds =
                worthMap.filter { it.value >= AdProbability.fromDisplayName(it.key.probability)?.acceptableValue!! }
                    .entries.sortedByDescending { it.value }
            val easyAds = groupedAdProbabilities[ProbabilityLevel.EASY.name]

            if (acceptableAds.isEmpty()) {
                val acceptableEasyAds = easyAds?.filter {
                    AdProbability.fromDisplayName(it.probability)?.acceptableValue!! > worthMap.getValue(it) - 8
                }?.filter { it.reward.toInt() > HPOT.price - gameState.gold }

                val easyAd = acceptableEasyAds?.firstOrNull()

                if (easyAd != null) {
                    return SuggestedMove("solve", adIds = listOf(easyAd.adId))
                }

                if (gameState.turn < 40 && gameState.gold > 100) {
                    return SuggestedMove("buy", ShopItemEnum.CS.id)
                }
            } else {
                if (easyAds?.isEmpty()!!) {
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

        return null
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