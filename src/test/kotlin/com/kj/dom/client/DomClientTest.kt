package com.kj.dom.client

import com.kj.dom.model.response.GameStartResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClient

@ExtendWith(MockitoExtension::class)
class DomClientTest {
  @Mock
  lateinit var restClient: RestClient

  @InjectMocks
  lateinit var domApiClient: DomApiClient

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
    whenever(responseSpecMock.body(GameStartResponse::class.java)).thenReturn(expectedResponse)

    // when
    val result = domApiClient.startGame()

    // then
    assertThat(result).isEqualTo(expectedResponse)
  }
}
