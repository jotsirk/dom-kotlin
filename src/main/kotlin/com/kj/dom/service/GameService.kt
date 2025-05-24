package com.kj.dom.service

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.Champion
import com.kj.dom.model.GameState
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.SolveAd
import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.model.response.ShopResponse
import com.kj.dom.model.response.SolveAdResponse
import com.kj.dom.service.helper.GameUtil
import java.util.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class GameService {

    @Autowired
    private lateinit var domRestClient: RestClient

    fun getShopItems(gameId: String): ShopResponse {
        return fetchShopItems(gameId)
            ?: throw IllegalStateException("Could not fetch shop items")
    }

    fun startGame(): GameState {
        val gameStateResponse = sendStartGameRequest()
            ?: throw IllegalStateException("Could not start game")
        return gameStateResponse.toModel()
    }

    fun getAdMessages(gameId: String): List<AdMessage> {
        val response = fetchAdMessages(gameId)
        log.debug("Ad messages response for gameId {}: {}", gameId, response)

        return response?.mapNotNull { adResponse ->
            if (adResponse.encrypted == 2) {
                log.debug("Skipping ad with encryption type 2 for gameId {}: {}", gameId, adResponse)
                return@mapNotNull null
            }

            val decryptedAdId = if (adResponse.encrypted == 1 && isBase64(adResponse.adId)) decodeBase64(adResponse.adId) else adResponse.adId
            val decryptedMessage = if (adResponse.encrypted == 1 && isBase64(adResponse.message)) decodeBase64(adResponse.message) else adResponse.message
            val decryptedProbability = if (adResponse.encrypted == 1 && isBase64(adResponse.probability)) decodeBase64(adResponse.probability) else adResponse.probability

            val enumProbability = AdProbability.fromDisplayName(decryptedProbability)

            AdMessage(
                adId = decryptedAdId,
                message = decryptedMessage,
                reward = adResponse.reward.toInt(),
                expiresIn = adResponse.expiresIn,
                encrypted = adResponse.encrypted,
                probability = enumProbability?.displayName ?: decryptedProbability,
            )
        } ?: throw IllegalStateException("Could not fetch ad messages")
    }

    fun buyShopItem(gameId: String, itemId: String): ShopBuyResponse {
        val response = buyShopItemRequest(gameId, itemId)
            ?: throw IllegalStateException("Could not buy shop item")
        // TODO tomodel
        return response
    }

    fun solveAd(gameId: String, adId: String): SolveAd {
        val response = solveAdRequest(gameId, adId) ?: throw IllegalStateException("Could not solve ad")
        return response.toModel()
    }

    fun solveGame(champion: Champion): Champion {
        log.info("Starting game: ${champion.gameState.gameId}")

        champion.isGameRunning = true

        while (champion.isGameRunning) {
            val adsList = getAdMessages(champion.gameState.gameId)
            val suggestedMove = GameUtil.calculateSuggestedMove(adsList, champion.gameState)

            log.debug("Move found - will try and execute: {}", suggestedMove)

            when (suggestedMove.move) {
                SOLVE -> {
                    val adId = suggestedMove.adIds.first()
                    val solveAdResponse = solveAd(champion.gameState.gameId, adId)
                    val newGameState = champion.gameState.copy(
                        lives = solveAdResponse.lives,
                        gold = solveAdResponse.gold,
                        score = solveAdResponse.score,
                        turn = solveAdResponse.turn,
                    )

                    champion.gameState = newGameState
                    champion.moves.add(suggestedMove)
                }

                WAIT -> {
                    val shopBuyResponse = buyShopItem(champion.gameState.gameId, HPOT.id)
                    val newGameState = champion.gameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )

                    champion.gameState = newGameState
                    champion.moves.add(suggestedMove)
                }

                BUY -> {
                    val shopBuyResponse = buyShopItem(champion.gameState.gameId, suggestedMove.itemId!!)
                    val newGameState = champion.gameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )

                    champion.gameState = newGameState
                    champion.moves.add(suggestedMove)
                    champion.items.add(suggestedMove.itemId)
                }

                SOLVE_MULTIPLE -> {
                    val adIds = suggestedMove.adIds
                    adIds.forEach {
                        val solveAdResponse = solveAd(champion.gameState.gameId, it)
                        val newGameState = champion.gameState.copy(
                            lives = solveAdResponse.lives,
                            gold = solveAdResponse.gold,
                            score = solveAdResponse.score,
                            turn = solveAdResponse.turn,
                        )

                        champion.gameState = newGameState
                    }
                    champion.moves.add(suggestedMove)
                }
            }

            if (champion.gameState.lives == 0) {
                log.info("Game over! Finished game with score ${champion.gameState.score}, on turn ${champion.gameState.turn}")
                champion.isGameRunning = false
            }
        }

        return champion
    }

    private fun fetchShopItems(gameId: String): ShopResponse? {
        return domRestClient.get()
            .uri("/{gameId}/shop", gameId)
            .retrieve()
            .body(ShopResponse::class.java)
    }

    private fun fetchAdMessages(gameId: String): List<AdMessageResponse>? {
        return domRestClient.get()
            .uri("/{gameId}/messages", gameId)
            .retrieve()
            .body<List<AdMessageResponse>>()
    }

    private fun sendStartGameRequest(): GameStartResponse? {
        return domRestClient.post()
            .uri("/game/start")
            .retrieve()
            .body(GameStartResponse::class.java)
    }

    private fun buyShopItemRequest(gameId: String, itemId: String): ShopBuyResponse? {
        return domRestClient.post()
            .uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
            .retrieve()
            .body(ShopBuyResponse::class.java)
    }

    private fun solveAdRequest(gameId: String, adId: String): SolveAdResponse? =
        domRestClient.post()
            .uri("/{gameId}/solve/{adId}", gameId, adId)
            .retrieve()
            .body(SolveAdResponse::class.java)

    private fun isBase64(value: String): Boolean {
        return try {
            Base64.getDecoder().decode(value)
            true
        } catch (e: IllegalArgumentException) {
            log.error("Encoding is not base64, skipping ad", e)
            false
        }
    }

    private fun decodeBase64(encoded: String): String {
        return String(Base64.getDecoder().decode(encoded))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GameService::class.java)
    }
}