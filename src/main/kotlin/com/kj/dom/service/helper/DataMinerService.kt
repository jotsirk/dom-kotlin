package com.kj.dom.service.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kj.dom.model.AdMessage
import com.kj.dom.model.GameState
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
    private val probabilityTurnCount = 100

    fun dataMine() {
        gameSerivce.startGame()

        val adProbabilitiesGroup = mutableSetOf<String>()
        val adDataLogList = mutableListOf<AdData>()
        val gameState = gameSerivce.getGameState()

        mineAdData(gameState, adProbabilitiesGroup, adDataLogList)
        writeAdDataToFile(adDataLogList, adProbabilitiesGroup)

        mineProbabilityPercentage(gameState, adProbabilitiesGroup)
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
        val unfinishedProbabilityResults = adProbabilitiesGroup.associateWith { ProbabilityData(0, 0) }.toMutableMap()
        val finishedProbabilityResults = mutableMapOf<String, ProbabilityData>()
        var currenGameState = gameState

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
                log.info("No valid ads found for current unfinished probabilities. Skipping day and buying items.")
                try {
                    gameSerivce.buyShopItem(currenGameState.gameId, HPOT.name)
                } catch (e: Exception) {
                    log.warn("Failed to buy shop item for game {}: {}", currenGameState.gameId, e.message)
                }
                continue
            }

            val probability = validProbabilities.first()
            val ad = adsList.firstOrNull { it.probability == probability }

            if (ad != null) {
                val solvedAd = gameSerivce.solveAd(currenGameState.gameId, ad.adId)

                if (solvedAd.lives == 0) {
                    log.info("Player died (score: {}). Restarting the game.", currenGameState.score)

                    updateProbabilityData(probability, unfinishedProbabilityResults, false)

                    gameSerivce.startGame()
                    currenGameState = gameSerivce.getGameState()
                    continue
                } else {
                    updateProbabilityData(probability, unfinishedProbabilityResults, true)
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