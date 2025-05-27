package com.kj.dom.service

import com.kj.dom.client.DomApiClient
import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.model.response.SolveAdResponse
import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

@ExtendWith(MockitoExtension::class)
class GameServiceTest {
  @Mock
  private lateinit var domApiClient: DomApiClient

  @InjectMocks
  private lateinit var gameService: GameService

  @Test
  fun `startGame - returns GameState - if no errors`() {
    // given
    val gameStartResponse =
      GameStartResponse(
        gameId = "1234",
        lives = 3,
        gold = 0,
        level = 0,
        score = 0,
        highScore = 0,
        turn = 0,
      )

    whenever(domApiClient.startGame()).thenReturn(gameStartResponse)

    // when
    val result = gameService.startGame()

    // then
    assertThat(result).isEqualTo(gameStartResponse.toModel())

    verify(domApiClient).startGame()
  }

  @Test
  fun `startGame - throws IllegalArgumentException - if domApiClient response is null`() {
    // given
    whenever(domApiClient.startGame()).thenReturn(null)

    // when
    val result = catchThrowable { gameService.startGame() }

    // then
    assertThat(result).isInstanceOfSatisfying(IllegalStateException::class.java) {
      assertThat(it.message).isEqualTo("Could not start game")
    }
  }

  @Test
  fun `startGame - throws HttpClientException - if server has an exception`() {
    // given
    whenever(domApiClient.startGame()).thenThrow(
      HttpClientErrorException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
      ),
    )

    // when
    val result = catchThrowable { gameService.startGame() }

    // then
    assertThat(result).isInstanceOfSatisfying(HttpClientErrorException::class.java) {
      assertThat(it.message).isEqualTo("500 Internal Server Error")
    }
  }

  @Test
  fun `getAdMessage - returns list of 3 ads - if no errors`() {
    // given
    val gameId = "game123"
    val adMessageResponse1 =
      AdMessageResponse(
        adId = "ad123",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )
    val adMessageResponse2 =
      AdMessageResponse(
        adId = "ad124",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )
    val adMessageResponse3 =
      AdMessageResponse(
        adId = "ad125",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )

    val adsList =
      listOf(
        adMessageResponse1,
        adMessageResponse2,
        adMessageResponse3,
      )

    whenever(domApiClient.findAdMessages(gameId)).thenReturn(adsList)

    // when
    val result = gameService.getAdMessages(gameId)

    // then
    assertThat(result.size).isEqualTo(3)
    assertThat(result.map { it.adId }).containsExactly("ad123", "ad124", "ad125")

    verify(domApiClient).findAdMessages(gameId)
  }

  @Test
  fun `getAdMessage - returns list of 2 ads - if no errors and one ad is encryption 2`() {
    // given
    val gameId = "game123"
    val adMessageResponse1 =
      AdMessageResponse(
        adId = "ad123",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )
    val adMessageResponse2 =
      AdMessageResponse(
        adId = "ad124",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = 2,
        probability = "Sure thing",
      )
    val adMessageResponse3 =
      AdMessageResponse(
        adId = "ad125",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )

    val adsList =
      listOf(
        adMessageResponse1,
        adMessageResponse2,
        adMessageResponse3,
      )

    whenever(domApiClient.findAdMessages(gameId)).thenReturn(adsList)

    // when
    val result = gameService.getAdMessages(gameId)

    // then
    assertThat(result.size).isEqualTo(2)
    assertThat(result.map { it.adId }).containsExactly("ad123", "ad125")

    verify(domApiClient).findAdMessages(gameId)
  }

  @Test
  fun `getAdMessage - returns list of unencrypted 3 ads - if no errors and 2 ads are encryption 1`() {
    // given
    val gameId = "game123"
    val adMessageResponse1 =
      AdMessageResponse(
        adId = Base64.getEncoder().encodeToString("ad123".toByteArray()),
        message = Base64.getEncoder().encodeToString("msg1".toByteArray()),
        reward = "10",
        expiresIn = 7,
        encrypted = 1,
        probability = Base64.getEncoder().encodeToString("Sure thing".toByteArray()),
      )
    val adMessageResponse2 =
      AdMessageResponse(
        adId = Base64.getEncoder().encodeToString("ad124".toByteArray()),
        message = Base64.getEncoder().encodeToString("msg1".toByteArray()),
        reward = "10",
        expiresIn = 7,
        encrypted = 1,
        probability = Base64.getEncoder().encodeToString("Sure thing".toByteArray()),
      )
    val adMessageResponse3 =
      AdMessageResponse(
        adId = "ad125",
        message = "msg1",
        reward = "10",
        expiresIn = 7,
        encrypted = null,
        probability = "Sure thing",
      )

    val adsList =
      listOf(
        adMessageResponse1,
        adMessageResponse2,
        adMessageResponse3,
      )

    whenever(domApiClient.findAdMessages(gameId)).thenReturn(adsList)

    // when
    val result = gameService.getAdMessages(gameId)

    // then
    assertThat(result.size).isEqualTo(3)
    assertThat(result.map { it.adId }).containsExactly("ad123", "ad124", "ad125")

    verify(domApiClient).findAdMessages(gameId)
  }

  @Test
  fun `buyShopItem - returns ShopBuyResponse - if no errors`() {
    // given
    val gameId = "game123"
    val itemId = "cs"
    val expectedResponse = ShopBuyResponse(lives = 3, gold = 50, turn = 2, shoppingSuccess = true)

    whenever(domApiClient.buyShopItem(gameId, itemId)).thenReturn(expectedResponse)

    // when
    val result = gameService.buyShopItem(gameId, itemId)

    // then
    assertThat(result).isEqualTo(expectedResponse)
    verify(domApiClient).buyShopItem(gameId, itemId)
  }

  @Test
  fun `buyShopItem - throws exception - if domApiClient response is null`() {
    // given
    val gameId = "game123"
    val itemId = "cs"

    whenever(domApiClient.buyShopItem(gameId, itemId)).thenReturn(null)

    // when
    val result = catchThrowable { gameService.buyShopItem(gameId, itemId) }

    // then
    assertThat(result).isInstanceOfSatisfying(IllegalStateException::class.java) {
      assertThat(it.message).isEqualTo("Could not buy shop item")
    }

    verify(domApiClient).buyShopItem(gameId, itemId)
  }

  @Test
  fun `solveAd - returns SolveAd - if no errors`() {
    // given
    val gameId = "game123"
    val adId = "ad123"
    val response =
      SolveAdResponse(lives = 2, gold = 60, score = 15, turn = 3, success = true, highScore = 10, message = "")

    whenever(domApiClient.solveAd(gameId, adId)).thenReturn(response)

    // when
    val result = gameService.solveAd(gameId, adId)

    // then
    assertThat(result).isEqualTo(response.toModel())
    verify(domApiClient).solveAd(gameId, adId)
  }

  @Test
  fun `solveAd - throws exception - if domApiClient response is null`() {
    // given
    val gameId = "game123"
    val adId = "ad123"

    whenever(domApiClient.solveAd(gameId, adId)).thenReturn(null)

    // when
    val result = catchThrowable { gameService.solveAd(gameId, adId) }

    // then
    assertThat(result).isInstanceOfSatisfying(IllegalStateException::class.java) {
      assertThat(it.message).isEqualTo("Could not solve ad")
    }

    verify(domApiClient).solveAd(gameId, adId)
  }
}
