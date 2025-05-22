package com.kj.dom.service.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.AdProbability.PIECE_OF_CAKE
import com.kj.dom.model.GameState
import com.kj.dom.model.ShopItemEnum
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.service.GameService
import java.io.File
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataMinerService {

    @Autowired
    private lateinit var gameSerivce: GameService

    @Autowired
    private lateinit var log: Logger

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerKotlinModule()
    private val adMinerTurnCount = 100
    private val probabilityTurnCount = 500

    fun dataMine() {
        gameSerivce.startGame()

//        val adProbabilitiesGroup = mutableSetOf<String>()
//        val adDataLogList = mutableListOf<AdData>()

        val gameState = gameSerivce.getGameState()

//        mineAdData(gameState, adProbabilitiesGroup, adDataLogList)
//        writeAdDataToFile(adDataLogList, adProbabilitiesGroup)

        val probabilitiesList = AdProbability.entries.map { it.displayName }.toSet()
        mineProbabilityPercentage(gameState, probabilitiesList)
        dataMineItemProbabilities(gameState, PIECE_OF_CAKE)
    }

    private fun mineAdData(
        gameState: GameState,
        adProbabilitiesGroup: MutableSet<String>,
        adDataLogList: MutableList<AdData>
    ) {
        log.info("Starting turn {}", gameState.turn)

        if (gameState.turn >= adMinerTurnCount) {
            log.info("Game finished for ad mining: Reached turn limit of {}", adMinerTurnCount)
            return
        }

        val adMessages = gameSerivce.getAdMessages(gameState.gameId)
        adMessages.forEach { adMessage ->
            adProbabilitiesGroup.add(adMessage.probability)

            adDataLogList.add(
                AdData(
                    adCount = adMessages.count(),
                    turnCount = gameState.turn,
                    ads = adMessages
                )
            )
        }

        var shopBuyResponse: ShopBuyResponse?

        try {
            shopBuyResponse = gameSerivce.buyShopItem(gameState.gameId, HPOT.name)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw e
        }

        val newGameState = gameState.copy(
            gold = shopBuyResponse.gold,
            lives = shopBuyResponse.lives,
            turn = shopBuyResponse.turn,
        )

        mineAdData(newGameState, adProbabilitiesGroup, adDataLogList)
    }

    private fun writeAdDataToFile(
        adDataLogList: List<AdData>,
        adProbabilitiesGroup: Set<String>
    ) {
        val outputDir = File("logs")
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }

        val adDataFile = File(outputDir, "ad_data_log.json")
        val adProbabilitiesFile = File(outputDir, "ad_probabilities.txt")

        adDataFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(adDataLogList))

        adProbabilitiesFile.printWriter().use { writer ->
            writer.println("Ad Probabilities (Unique):")
            adProbabilitiesGroup.forEach { writer.println(it) }
        }

        log.info(
            "Ad mining details written to files: {}, {}",
            adDataFile.absolutePath,
            adProbabilitiesFile.absolutePath
        )
    }

    private fun mineProbabilityPercentage(gameState: GameState, adProbabilitiesGroup: Set<String>) {
        val unfinishedProbabilityResults = adProbabilitiesGroup
            .filter { it == AdProbability.PLAYING_WITH_FIRE.displayName }
            .associateWith { ProbabilityData(0, 0) }.toMutableMap()
        val finishedProbabilityResults = mutableMapOf<String, ProbabilityData>()
        var currenGameState = gameState
        var consecutiveSkips = 0

        while (unfinishedProbabilityResults.isNotEmpty()) {
            var adsList: List<AdMessage>

            try {
                adsList = gameSerivce.getAdMessages(currenGameState.gameId)
            } catch (e: Exception) {
                log.error(
                    "Game {} probably over or encountered an error. Restarting game. Exception: {}",
                    currenGameState.gameId, e.message
                )

                gameSerivce.startGame()
                currenGameState = gameSerivce.getGameState()
                continue
            }

            val adsProbabilities = adsList.map { it.probability }.toSet()
            val validProbabilities = unfinishedProbabilityResults.keys.intersect(adsProbabilities)

            if (validProbabilities.isEmpty()) {
                consecutiveSkips++
                log.info("No valid ads found for current unfinished probabilities. Skipping day and buying items. (Consecutive skips: {})", consecutiveSkips)

                if (consecutiveSkips >= 100) {
                    log.warn("Maximum consecutive skips reached (100). Terminating probability mining.")
                    break
                }

                try {
                    gameSerivce.buyShopItem(currenGameState.gameId, HPOT.displayName)
                } catch (e: Exception) {
                    log.warn("Failed to buy shop item for game {}: {}", currenGameState.gameId, e.message)
                }
                continue
            } else {
                consecutiveSkips = 0
            }

            val probability = validProbabilities.first()
            val ad = adsList.firstOrNull { it.probability == probability }

            if (ad != null) {
                val solvedAd = gameSerivce.solveAd(currenGameState.gameId, ad.adId)

                if (solvedAd.lives == 0) {
                    log.info("Player died (score: {}). Restarting the game.", solvedAd.score)

                    updateProbabilityData(probability, unfinishedProbabilityResults, false)

                    gameSerivce.startGame()
                    currenGameState = gameSerivce.getGameState()
                    continue
                } else {
                    if (solvedAd.success) {
                        updateProbabilityData(probability, unfinishedProbabilityResults, true)
                    } else {
                        updateProbabilityData(probability, unfinishedProbabilityResults, false)
                    }
                }
            } else {
                log.info("Could not process ad with probability {}. Skipping day and buying shop item.", probability)

                try {
                    gameSerivce.buyShopItem(currenGameState.gameId, HPOT.name)
                } catch (e: Exception) {
                    log.warn("Failed to buy shop item for game {}: {}", currenGameState.gameId, e.message)
                }
            }

            val probabilityData = unfinishedProbabilityResults[probability]!!
            if (probabilityData.runCount >= probabilityTurnCount) {
                finishedProbabilityResults[probability] = probabilityData
                unfinishedProbabilityResults.remove(probability)
            }
        }

        logFinishedProbabilityResults(finishedProbabilityResults)
    }


    private fun updateProbabilityData(
        probability: String,
        results: MutableMap<String, ProbabilityData>,
        wasSuccessful: Boolean
    ) {
        val probabilityData = results[probability] ?: return
        results[probability] = probabilityData.copy(
            runCount = probabilityData.runCount + 1,
            successCount = probabilityData.successCount + if (wasSuccessful) 1 else 0
        )
    }

    private fun logFinishedProbabilityResults(finishedResults: Map<String, ProbabilityData>) {
        val outputDir = File("logs")
        if (!outputDir.exists()) outputDir.mkdir()

        val resultsFile = File(outputDir, "finished_probability_results.json")
        resultsFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finishedResults))

        log.info("Finished probability results saved to {}", resultsFile.absolutePath)
    }

    private fun dataMineItemProbabilities(gameState: GameState, probability: AdProbability) {
        val clearRunProbabilityData = ClearRunProbabilityData(probability, 0, 0)
        val itemRunList = mutableListOf<ItemRunProbablityData>()

        startClearRun(gameState, clearRunProbabilityData)

        val shopItems = ShopItemEnum.entries.filterNot { it.name == HPOT.name }
        shopItems.forEach {
            val itemRunProbabilityData = ItemRunProbablityData(it, probability, 0, 0)

            startItemRun(itemRunProbabilityData)
            itemRunList.add(itemRunProbabilityData)
        }

        writeResultsToFile(probability, clearRunProbabilityData, itemRunList)
    }


    private fun startClearRun(gameState: GameState, clearRunProbabilityData: ClearRunProbabilityData) {
        var currentGameState = gameState

        while (clearRunProbabilityData.runCount < probabilityTurnCount) {
            try {
                val adsList = gameSerivce.getAdMessages(currentGameState.gameId)

                val targetAd = adsList.firstOrNull { it.probability == clearRunProbabilityData.probability.displayName }

                if (targetAd != null) {
                    val solveAdResponse = gameSerivce.solveAd(currentGameState.gameId, targetAd.adId)

                    if (solveAdResponse.lives == 0) {
                        log.info("User died while solving ad with probability {}. Restarting game.", targetAd.probability)
                        clearRunProbabilityData.runCount++
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                        continue
                    }

                    if (solveAdResponse.success) clearRunProbabilityData.successCount++

                    clearRunProbabilityData.runCount++

                    log.info(
                        "Solved ad with probability {}. RunCount: {}, SuccessCount: {}",
                        targetAd.probability,
                        clearRunProbabilityData.runCount,
                        clearRunProbabilityData.successCount
                    )
                } else {
                    log.info("Target probability {} not found in ads list. Buying life from shop.", clearRunProbabilityData.probability)

                    try {
                        val buyShopResponse = gameSerivce.buyShopItem(currentGameState.gameId, HPOT.displayName)
                        currentGameState = currentGameState.copy(
                            lives = buyShopResponse.lives,
                            gold = buyShopResponse.gold,
                            turn = buyShopResponse.turn
                        )
                    } catch (e: Exception) {
                        log.error("Failed to buy health potion. Restarting game.", e)
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                    }
                }
            } catch (e: Exception) {
                log.error("Encountered an error during ad processing or game state update. Restarting game.", e)
                gameSerivce.startGame()
                currentGameState = gameSerivce.getGameState()
            }
        }

        log.info(
            "Finished clear run for probability {}. Total Runs: {}, Total Successes: {}.",
            clearRunProbabilityData.probability,
            clearRunProbabilityData.runCount,
            clearRunProbabilityData.successCount
        )
    }

    private fun startItemRun(itemRunProbabilityData: ItemRunProbablityData) {
        gameSerivce.startGame()
        var currentGameState = gameSerivce.getGameState()

        val itemPrice = itemRunProbabilityData.item.price
        log.info("Starting item run for {} with target probability {}. Collecting gold to buy the item (Cost: {}).",
            itemRunProbabilityData.item, itemRunProbabilityData.probability.displayName, itemPrice)

        while (currentGameState.gold < itemPrice) {
            try {
                val adsList = gameSerivce.getAdMessages(currentGameState.gameId)

                val randomAd = adsList.firstOrNull()
                if (randomAd != null) {
                    val solveAdResponse = gameSerivce.solveAd(currentGameState.gameId, randomAd.adId)
                    currentGameState = currentGameState.copy(
                        lives = solveAdResponse.lives,
                        gold = solveAdResponse.gold,
                        score = solveAdResponse.score
                    )

                    if (solveAdResponse.lives == 0) {
                        log.info("Player died while collecting gold. Restarting game.")
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                        continue
                    }
                    log.info("Collected gold: {} (Lives: {}, Turn: {}).", currentGameState.gold, currentGameState.lives, currentGameState.turn)
                } else {
                    log.info("No ads available to collect gold. Skipping turn.")
                }
            } catch (e: Exception) {
                log.error("Error while collecting gold. Restarting game.", e)
                gameSerivce.startGame()
                currentGameState = gameSerivce.getGameState()
            }
        }

        try {
            val shopBuyResponse = gameSerivce.buyShopItem(currentGameState.gameId, itemRunProbabilityData.item.displayName)
            currentGameState = currentGameState.copy(
                lives = shopBuyResponse.lives,
                gold = shopBuyResponse.gold,
                turn = shopBuyResponse.turn
            )
            log.info("Successfully bought item {}. Gold remaining: {}, Lives: {}.",
                itemRunProbabilityData.item, currentGameState.gold, currentGameState.lives)
        } catch (e: Exception) {
            log.error("Failed to buy item {}. Restarting game.", itemRunProbabilityData.item, e)
            gameSerivce.startGame()
            currentGameState = gameSerivce.getGameState()
            return
        }

        log.info("Starting to solve ads for probability {} with item {}.",
            itemRunProbabilityData.probability.displayName, itemRunProbabilityData.item)

        var notFoundCounter = 0

        while (itemRunProbabilityData.runCount < probabilityTurnCount) {
            try {
                val adsList = gameSerivce.getAdMessages(currentGameState.gameId)
                val targetAd = adsList.firstOrNull { it.probability == itemRunProbabilityData.probability.name }

                if (targetAd != null) {
                    val solveAdResponse = gameSerivce.solveAd(currentGameState.gameId, targetAd.adId)

                    if (solveAdResponse.lives == 0) {
                        log.info("Player died while solving ad with probability {}. Restarting run.", targetAd.probability)
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                        return startItemRun(itemRunProbabilityData)
                    }

                    if (solveAdResponse.success) itemRunProbabilityData.successCount++

                    itemRunProbabilityData.runCount++

                    currentGameState = currentGameState.copy(
                        lives = solveAdResponse.lives,
                        gold = solveAdResponse.gold,
                        score = solveAdResponse.score
                    )
                    log.info(
                        "Successfully solved ad with probability {}. RunCount: {}, SuccessCount: {}, Gold: {}, Lives: {}.",
                        targetAd.probability, itemRunProbabilityData.runCount, itemRunProbabilityData.successCount,
                        currentGameState.gold, currentGameState.lives
                    )
                    notFoundCounter = 0
                } else {
                    notFoundCounter++
                    log.info(
                        "Target probability {} not found in ads (Turn: {}). NotFoundCount: {}.",
                        itemRunProbabilityData.probability.displayName, currentGameState.turn, notFoundCounter
                    )

                    if (notFoundCounter >= 15) {
                        log.info("Target probability {} not found for 15 consecutive turns. Restarting game.", itemRunProbabilityData.probability.displayName)
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                        return startItemRun(itemRunProbabilityData)
                    }

                    try {
                        val shopBuyResponse = gameSerivce.buyShopItem(currentGameState.gameId, HPOT.displayName)
                        currentGameState = currentGameState.copy(
                            lives = shopBuyResponse.lives,
                            gold = shopBuyResponse.gold,
                            turn = shopBuyResponse.turn
                        )
                    } catch (e: Exception) {
                        log.error("Failed to buy life. Restarting game.", e)
                        gameSerivce.startGame()
                        currentGameState = gameSerivce.getGameState()
                        return startItemRun(itemRunProbabilityData)
                    }
                }
            } catch (e: Exception) {
                log.error("Error during ad processing. Restarting game.", e)
                gameSerivce.startGame()
                currentGameState = gameSerivce.getGameState()
                return startItemRun(itemRunProbabilityData)
            }
        }


        log.info("Finished item run for {} with probability {}. Total Runs: {}, Total Successes: {}.",
            itemRunProbabilityData.item, itemRunProbabilityData.probability.displayName,
            itemRunProbabilityData.runCount, itemRunProbabilityData.successCount)
    }

    private fun writeResultsToFile(
        probability: AdProbability,
        clearRunProbabilityData: ClearRunProbabilityData,
        itemRunList: List<ItemRunProbablityData>
    ) {
        val outputDir = File("logs")
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }

        val fileName = "probability_results_${probability.name.lowercase()}.json"
        val file = File(outputDir, fileName)

        val dataToWrite = mapOf(
            "clearRunProbabilityData" to clearRunProbabilityData,
            "itemRunList" to itemRunList
        )

        try {
            file.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataToWrite))
            log.info("Results for probability '{}' written to file: {}", probability.name, file.absolutePath)
        } catch (e: Exception) {
            log.error("Failed to write results for probability '{}' to file", probability.name, e)
        }
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