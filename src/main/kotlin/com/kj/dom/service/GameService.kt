package com.kj.dom.service

import com.kj.dom.model.GameState
import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

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

    fun getAdMessages(gameId: String): AdMessageResponse {
        val response = fetchAdMessages(gameId)
        log.debug("Ad messages response for gameId {}: {}", gameId, response)
        return response ?: throw IllegalStateException("Could not fetch ad messages")
    }

    fun buyShopItem(gameId: String, itemId: String) {

    }

    private fun fetchShopItems(gameId: String): ShopResponse? {
        return domRestClient.get()
            .uri("/{gameId}/shop", gameId)
            .retrieve()
            .body(ShopResponse::class.java)
    }

    private fun fetchAdMessages(gameId: String): AdMessageResponse? {
        return domRestClient.get()
            .uri("/{gameId}/messages", gameId)
            .retrieve()
            .body(AdMessageResponse::class.java)
    }

    private fun sendStartGameRequest(): GameStartResponse? {
        return domRestClient.post()
            .uri("/game/start")
            .retrieve()
            .body(GameStartResponse::class.java)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GameService::class.java)
    }
}