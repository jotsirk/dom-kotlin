package com.kj.dom.service

import com.kj.dom.model.response.GameStartResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class GameService {

    @Autowired
    private lateinit var domRestClient: RestClient

    fun startGame() {
        // TODO start the initial game and get the response
        val gameState = sendStartGameRequest()
    }

    private fun sendStartGameRequest(): GameStartResponse? {
        return domRestClient.post()
            .uri("/games/start")
            .retrieve()
            .body(GameStartResponse::class.java)
    }
}