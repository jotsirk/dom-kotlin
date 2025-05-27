package com.kj.dom.client

import com.kj.dom.model.response.AdMessageResponse
import com.kj.dom.model.response.GameStartResponse
import com.kj.dom.model.response.ShopBuyResponse
import com.kj.dom.model.response.SolveAdResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders.EMPTY
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

@ExtendWith(MockitoExtension::class)
class DomClientTest {
  @Mock
  private lateinit var restClient: RestClient

  @InjectMocks
  private lateinit var domApiClient: DomApiClient

  @Test
  fun `startGame - returns GameStartResponse - if no errors`() {
    // given
    val expectedResponse =
      GameStartResponse(gameId = "1234", lives = 3, gold = 0, level = 0, score = 0, highScore = 0, turn = 0)
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/game/start")).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<GameStartResponse>() {}))
      .thenReturn(expectedResponse)

    // when
    val result = domApiClient.startGame()

    // then
    assertThat(result).isEqualTo(expectedResponse)

    verify(restClient).post()
    verify(uriSpecMock).uri("/game/start")
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<GameStartResponse>() {})
  }

  @Test
  fun `startGame - throws HttpServerErrorException 500 exception - server is unreachable`() {
    // given
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val http500Exception =
      HttpServerErrorException.create(
        INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        EMPTY,
        "Server failed".toByteArray(),
        null,
      )

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/game/start")).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenThrow(http500Exception)

    // when
    val result = catchThrowable { domApiClient.startGame() }

    // then
    assertThat(result).isInstanceOfSatisfying(HttpServerErrorException::class.java) {
      assertThat(it.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    verify(restClient).post()
    verify(uriSpecMock).uri("/game/start")
    verify(uriSpecMock).retrieve()
  }

  @Test
  fun `startGame - returns null - when response body is null`() {
    // given
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/game/start")).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<GameStartResponse>() {}))
      .thenReturn(null)

    // when
    val result = domApiClient.startGame()

    // then
    assertThat(result).isNull()

    verify(restClient).post()
    verify(uriSpecMock).uri("/game/start")
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<GameStartResponse>() {})
  }

  @Test
  fun `findAdMessages - returns list of ads - if no errors`() {
    // given
    val gameId = "01JW62NM7EN30MXB7CBBF4808V"
    val expectedAds =
      listOf(
        AdMessageResponse(
          adId = "ad1",
          message = "msg1",
          reward = "10",
          expiresIn = 5,
          encrypted = 0,
          probability = "Sure thing",
        ),
        AdMessageResponse(
          adId = "ad2",
          message = "msg2",
          reward = "20",
          expiresIn = 3,
          encrypted = 0,
          probability = "Gamble",
        ),
      )
    val uriSpecMock: RestClient.RequestHeadersUriSpec<*> = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.get()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/messages", gameId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<List<AdMessageResponse>>() {}))
      .thenReturn(expectedAds)

    // when
    val result = domApiClient.findAdMessages(gameId)

    // then
    assertThat(result).isEqualTo(expectedAds)

    verify(restClient).get()
    verify(uriSpecMock).uri("/{gameId}/messages", gameId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<List<AdMessageResponse>>() {})
  }

  @Test
  fun `findAdMessages - throws HttpServerErrorException 500 exception - server is unreachable`() {
    // given
    val gameId = "01JW62NM7EN30MXB7CBBF4808V"
    val uriSpecMock: RestClient.RequestHeadersUriSpec<*> = mock()
    val http500Exception =
      HttpServerErrorException.create(
        INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        EMPTY,
        "Server failed".toByteArray(),
        null,
      )

    whenever(restClient.get()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/messages", gameId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenThrow(http500Exception)

    // when
    val result = catchThrowable { domApiClient.findAdMessages(gameId) }

    // then
    assertThat(result).isInstanceOfSatisfying(HttpServerErrorException::class.java) {
      assertThat(it.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    verify(restClient).get()
    verify(uriSpecMock).uri("/{gameId}/messages", gameId)
    verify(uriSpecMock).retrieve()
  }

  @Test
  fun `findAdMessages - returns null - when response body is null`() {
    // given
    val gameId = "01JW62NM7EN30MXB7CBBF4808V"
    val uriSpecMock: RestClient.RequestHeadersUriSpec<*> = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.get()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/messages", gameId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<List<AdMessageResponse>>() {}))
      .thenReturn(null)

    // when
    val result = domApiClient.findAdMessages(gameId)

    // then
    assertThat(result).isNull()

    verify(restClient).get()
    verify(uriSpecMock).uri("/{gameId}/messages", gameId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<List<AdMessageResponse>>() {})
  }

  @Test
  fun `solveAd - returns SolveAdResponse - if no errors`() {
    // given
    val gameId = "game123"
    val adId = "ad456"
    val expectedResponse =
      SolveAdResponse(lives = 2, gold = 50, score = 100, turn = 3, success = true, highScore = 0, message = "")
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/solve/{adId}", gameId, adId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<SolveAdResponse>() {})).thenReturn(
      expectedResponse,
    )

    // when
    val result = domApiClient.solveAd(gameId, adId)

    // then
    assertThat(result).isEqualTo(expectedResponse)

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/solve/{adId}", gameId, adId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<SolveAdResponse>() {})
  }

  @Test
  fun `solveAd - throws HttpServerErrorException 500 exception - server is unreachable`() {
    // given
    val gameId = "game123"
    val adId = "ad456"
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val http500Exception =
      HttpServerErrorException.create(
        INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        EMPTY,
        "Server error".toByteArray(),
        null,
      )

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/solve/{adId}", gameId, adId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenThrow(http500Exception)

    // when
    val result = catchThrowable { domApiClient.solveAd(gameId, adId) }

    // then
    assertThat(result).isInstanceOfSatisfying(HttpServerErrorException::class.java) {
      assertThat(it.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/solve/{adId}", gameId, adId)
    verify(uriSpecMock).retrieve()
  }

  @Test
  fun `solveAd - returns null - when response body is null`() {
    // given
    val gameId = "game123"
    val adId = "ad456"
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/solve/{adId}", gameId, adId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<SolveAdResponse>() {})).thenReturn(null)

    // when
    val result = domApiClient.solveAd(gameId, adId)

    // then
    assertThat(result).isNull()

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/solve/{adId}", gameId, adId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<SolveAdResponse>() {})
  }

  @Test
  fun `buyShopItem - returns ShopBuyResponse - if no errors`() {
    // given
    val gameId = "game123"
    val itemId = "hpot"
    val expectedResponse = ShopBuyResponse(lives = 2, gold = 40, turn = 5, shoppingSuccess = true)
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<ShopBuyResponse>() {}))
      .thenReturn(expectedResponse)

    // when
    val result = domApiClient.buyShopItem(gameId, itemId)

    // then
    assertThat(result).isEqualTo(expectedResponse)

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<ShopBuyResponse>() {})
  }

  @Test
  fun `buyShopItem - throws HttpServerErrorException 500 exception - server is unreachable`() {
    // given
    val gameId = "game123"
    val itemId = "hpot"
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val http500Exception =
      HttpServerErrorException.create(
        INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        EMPTY,
        "Server error".toByteArray(),
        null,
      )

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenThrow(http500Exception)

    // when
    val result = catchThrowable { domApiClient.buyShopItem(gameId, itemId) }

    // then
    assertThat(result).isInstanceOfSatisfying(HttpServerErrorException::class.java) {
      assertThat(it.statusCode).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
    verify(uriSpecMock).retrieve()
  }

  @Test
  fun `buyShopItem - returns null - when response body is null`() {
    // given
    val gameId = "game123"
    val itemId = "hpot"
    val uriSpecMock: RestClient.RequestBodyUriSpec = mock()
    val responseSpecMock: RestClient.ResponseSpec = mock()

    whenever(restClient.post()).thenReturn(uriSpecMock)
    whenever(uriSpecMock.uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)).thenReturn(uriSpecMock)
    whenever(uriSpecMock.retrieve()).thenReturn(responseSpecMock)
    whenever(responseSpecMock.body(object : ParameterizedTypeReference<ShopBuyResponse>() {}))
      .thenReturn(null)

    // when
    val result = domApiClient.buyShopItem(gameId, itemId)

    // then
    assertThat(result).isNull()

    verify(restClient).post()
    verify(uriSpecMock).uri("/{gameId}/shop/buy/{itemId}", gameId, itemId)
    verify(uriSpecMock).retrieve()
    verify(responseSpecMock).body(object : ParameterizedTypeReference<ShopBuyResponse>() {})
  }
}
