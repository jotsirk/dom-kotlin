package com.kj.dom.client

import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.model.response.SolveAdResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class DomApiClient {
  @Autowired
  private lateinit var domRestClient: RestClient

  fun startGame(): GameStartResponse? =
    domRestClient
      .post()
      .uri("/game/start")
      .retrieve()
      .body()

  fun findAdMessages(gameId: String): List<AdMessageResponse>? =
    domRestClient
      .get()
      .uri("/{gameId}/messages", gameId)
      .retrieve()
      .body()

  fun solveAd(
    gameId: String,
    adId: String,
  ): SolveAdResponse? =
    domRestClient
      .post()
      .uri("/{gameId}/solve/{adId}", gameId, adId)
      .retrieve()
      .body()

  fun buyShopItem(
    gameId: String,
    itemId: String,
  ): ShopBuyResponse? =
    domRestClient
      .post()
      .uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
      .retrieve()
      .body()
}
