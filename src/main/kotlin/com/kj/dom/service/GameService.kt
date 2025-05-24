package com.kj.dom.service

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.AdProbability.QUITE_LIKELY
import com.kj.dom.model.Champion
import com.kj.dom.model.GameState
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.ShopItemEnum
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.SolveAd
import com.kj.dom.model.SuggestedMove
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

            val suggestedMove = calculateSuggestedMove(adsList, champion.gameState)

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
                    champion.moves.plus(suggestedMove)
                }

                WAIT -> {
                    val shopBuyResponse = buyShopItem(champion.gameState.gameId, HPOT.id)
                    val newGameState = champion.gameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )

                    champion.gameState = newGameState
                    champion.moves.plus(suggestedMove)
                }

                BUY -> {
                    val shopBuyResponse = buyShopItem(champion.gameState.gameId, suggestedMove.itemId!!)
                    val newGameState = champion.gameState.copy(
                        lives = shopBuyResponse.lives,
                        gold = shopBuyResponse.gold,
                        turn = shopBuyResponse.turn,
                    )

                    champion.gameState = newGameState
                    champion.moves.plus(suggestedMove)
                    champion.items.plus(suggestedMove.itemId)
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
                    champion.moves.plus(suggestedMove)
                }
            }

            if (champion.gameState.lives == 0) {
                log.info("Game over! Finished game with score ${champion.gameState.score}, on turn ${champion.gameState.turn}")
                champion.isGameRunning = false
            }
        }

        return champion
    }

    private fun calculateSuggestedMove(
        ads: List<AdMessage>,
        gameState: GameState,
    ): SuggestedMove {
        val groupedAdProbabilities = GameUtil.createAdProbabilityMap(ads)
        val worthMap: Map<AdMessage, Double> = ads.associateWith {
            GameUtil.calculateAdRewardWorth(
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
                val acceptableEasyAds = easyAds.filter {
                    val threshold = AdProbability.fromDisplayName(it.probability)?.acceptableValue ?: 0
                    val worth = worthMap[it] ?: 0.0
                    threshold > worth - 5
                }.filter {
                    it.reward > HPOT.price - gameState.gold
                }

                val easyAd = acceptableEasyAds.firstOrNull()
                if (easyAd != null) {
                    return SuggestedMove(SOLVE, adIds = listOf(easyAd.adId))
                }

                if (easyAds.isEmpty()) {
                    when {
                        gameState.gold > 300 -> return SuggestedMove(BUY, ShopItemEnum.MTRIX.id)
                        gameState.gold > 100 -> return SuggestedMove(BUY, ShopItemEnum.CS.id)
                        gameState.gold > 50 -> return SuggestedMove(BUY, ShopItemEnum.HPOT.id)
                        else -> {
                            val fallbackAd =
                                groupedAdProbabilities[MEDIUM.name].orEmpty().firstOrNull {
                                    it.probability in listOf(
                                        QUITE_LIKELY.displayName,
                                        AdProbability.HMMM.displayName
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
                    val mediumAds = groupedAdProbabilities[MEDIUM.name]

                    if (mediumAds?.isNotEmpty()!!) {
                        val acceptableMediumAd = mediumAds.firstOrNull {
                            val worth = worthMap[it] ?: return@firstOrNull false
                            val threshold =
                                AdProbability.fromDisplayName(it.probability)?.acceptableValue
                                    ?: return@firstOrNull false
                            worth >= threshold
                        }

                        if (acceptableMediumAd != null && acceptableMediumAd.probability == QUITE_LIKELY.displayName) {
                            return SuggestedMove(SOLVE, adIds = listOf(acceptableMediumAd.adId))
                        } else {
                            if (gameState.gold > 300) {
                                return SuggestedMove(BUY, ShopItemEnum.MTRIX.id)
                            } else if (gameState.gold > 100 && gameState.gold < 300) {
                                return SuggestedMove(BUY, ShopItemEnum.CS.id)
                            } else if (gameState.gold > 50 && gameState.gold < 100) {
                                return SuggestedMove(BUY, ShopItemEnum.HPOT.id)
                            } else {
                                val bestAd = acceptableAds.first()
                                return SuggestedMove(SOLVE, adIds = listOf(bestAd.key.adId))
                            }
                        }
                    } else {
                        if (gameState.gold > 300) {
                            return SuggestedMove(BUY, ShopItemEnum.MTRIX.id)
                        } else if (gameState.gold > 100 && gameState.gold < 300) {
                            return SuggestedMove(BUY, ShopItemEnum.CS.id)
                        } else if (gameState.gold > 50 && gameState.gold < 100) {
                            return SuggestedMove(BUY, ShopItemEnum.HPOT.id)
                        } else {
                            val bestAd = acceptableAds.first()
                            return SuggestedMove(SOLVE, adIds = listOf(bestAd.key.adId))
                        }
                    }
                } else {
                    val bestAd = acceptableAds.first()
                    return SuggestedMove(SOLVE, adIds = listOf(bestAd.key.adId))
                }
            }
        }
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