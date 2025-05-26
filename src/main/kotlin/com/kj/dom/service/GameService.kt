package com.kj.dom.service

import com.kj.dom.client.DomApiClient
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
import com.kj.dom.model.SuggestedMove
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.service.helper.GameUtil
import com.kj.dom.service.helper.GameUtil.decodeBase64
import java.util.Base64
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GameService {
  @Autowired
  private lateinit var log: Logger

  @Autowired
  private lateinit var domApiClient: DomApiClient

  fun startGame(): GameState {
    val gameStateResponse = domApiClient.startGame() ?: error("Could not start game")
    return gameStateResponse.toModel()
  }

  fun getAdMessages(gameId: String): List<AdMessage> {
    val response = domApiClient.findAdMessages(gameId)

    return response?.mapNotNull { adResponse ->
      if (adResponse.encrypted == 2) {
        log.debug("Skipping ad with encryption type 2 for gameId {}: {}", gameId, adResponse)
        return@mapNotNull null
      }

      val decryptedAdId = decodeAdMessage(adResponse.adId, adResponse.encrypted)
      val decryptedMessage = decodeAdMessage(adResponse.adId, adResponse.encrypted)
      val decryptedProbability = decodeAdMessage(adResponse.probability, adResponse.encrypted)

      val enumProbability = AdProbability.fromDisplayName(decryptedProbability)

      AdMessage(
        adId = decryptedAdId,
        message = decryptedMessage,
        reward = adResponse.reward.toInt(),
        expiresIn = adResponse.expiresIn,
        encrypted = adResponse.encrypted,
        probability = enumProbability?.displayName ?: decryptedProbability,
      )
    } ?: error("Could not fetch ad messages")
  }

  fun buyShopItem(
    gameId: String,
    itemId: String,
  ): ShopBuyResponse {
    val response =
      domApiClient.buyShopItem(gameId, itemId)
        ?: error("Could not buy shop item")
    return response
  }

  fun solveAd(gameId: String, adId: String): SolveAd {
    val response = domApiClient.solveAd(gameId, adId) ?: error("Could not solve ad")
    return response.toModel()
  }

  fun solveGame(champion: Champion): Champion {
    log.info("Starting game: ${champion.gameState.gameId}")

    champion.isGameRunning = true

    while (champion.isGameRunning) {
      val adsList = getAdMessages(champion.gameState.gameId)
      val suggestedMove = GameUtil.calculateSuggestedMove(adsList, champion.gameState)

      log.debug("Move found - will try and execute: {}", suggestedMove)

      doSuggestedMove(suggestedMove, champion)

      if (champion.gameState.lives == 0) {
        log.info("Game over! Finished game with score ${champion.gameState.score}, on turn ${champion.gameState.turn}")
        champion.isGameRunning = false
      }
    }

    return champion
  }

  private fun doSuggestedMove(
    suggestedMove: SuggestedMove,
    champion: Champion,
  ) {
    when (suggestedMove.move) {
      SOLVE -> {
        val adId = suggestedMove.adIds.first()
        val response = solveAd(champion.gameState.gameId, adId)
        champion.gameState = updateGameStateFromSolve(champion, response)
      }

      WAIT -> {
        val response = buyShopItem(champion.gameState.gameId, HPOT.id)
        champion.gameState = updateGameStateFromShop(champion, response)
      }

      BUY -> {
        val itemId = suggestedMove.itemId ?: error("BUY move must have itemId")
        val response = buyShopItem(champion.gameState.gameId, itemId)
        champion.gameState = updateGameStateFromShop(champion, response)
        champion.items.add(itemId)
      }

      SOLVE_MULTIPLE -> {
        suggestedMove.adIds.forEach { adId ->
          val response = solveAd(champion.gameState.gameId, adId)
          champion.gameState = updateGameStateFromSolve(champion, response)
        }
      }
    }

    champion.moves.add(suggestedMove)
  }

  private fun updateGameStateFromSolve(champion: Champion, response: SolveAd): GameState {
    return champion.gameState.copy(
      lives = response.lives,
      gold = response.gold,
      score = response.score,
      turn = response.turn
    )
  }

  private fun updateGameStateFromShop(champion: Champion, response: ShopBuyResponse): GameState {
    return champion.gameState.copy(
      lives = response.lives,
      gold = response.gold,
      turn = response.turn
    )
  }

  private fun decodeAdMessage(
    value: String,
    encrypted: Int?,
  ): String = if (encrypted == 1 && isBase64(value)) decodeBase64(value) else value

  private fun isBase64(value: String): Boolean {
    return try {
      Base64.getDecoder().decode(value)
      true
    } catch (e: IllegalArgumentException) {
      log.error("Encoding is not base64, skipping ad", e)
      false
    }
  }

}
