package com.kj.dom.service

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.GameState
import com.kj.dom.model.SolveAd
import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.model.response.ShopResponse
import com.kj.dom.model.response.SolveAdResponse
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

    private lateinit var gameState: GameState

    fun getGameState(): GameState {
        if (!::gameState.isInitialized) {
            throw IllegalStateException("Game state is not initialized")
        }
        return gameState
    }

    fun getShopItems(gameId: String): ShopResponse {
        return fetchShopItems(gameId)
            ?: throw IllegalStateException("Could not fetch shop items")
    }

    fun startGame() {
        val gameStateResponse = sendStartGameRequest()
            ?: throw IllegalStateException("Could not start game")
        gameState = gameStateResponse.toModel()
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

            val enumProbability = AdProbability.entries.firstOrNull { it.displayName == decryptedProbability }
                ?: decryptedProbability

            AdMessage(
                adId = decryptedAdId,
                message = decryptedMessage,
                reward = adResponse.reward,
                expiresIn = adResponse.expiresIn,
                encrypted = adResponse.encrypted,
                probability = enumProbability.toString()
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