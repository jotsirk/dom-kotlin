package com.kj.dom.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.slf4j.Logger

@ExtendWith(MockitoExtension::class)
class GameInitializerServiceTest {
  @Mock
  lateinit var gameRunnerService: GameRunnerService

  @Mock
  lateinit var log: Logger

  @InjectMocks
  lateinit var gameInitializerService: GameInitializerService

  @Test
  fun `run - delegates to gameRunnerService - startManually is false`() {
    // given
    val startManuallyField = GameInitializerService::class.java.getDeclaredField("startManually")
    startManuallyField.isAccessible = true
    startManuallyField.set(gameInitializerService, false)

    // when
    gameInitializerService.run(null)

    // then
    verify(gameRunnerService).executeGameTask()
    verify(log, never()).info(any())
  }

  @Test
  fun `run - logs info message - startManually is true`() {
    // given
    val startManuallyField = GameInitializerService::class.java.getDeclaredField("startManually")
    startManuallyField.isAccessible = true
    startManuallyField.set(gameInitializerService, true)

    // when
    gameInitializerService.run(null)

    // then
    verify(gameRunnerService, never()).executeGameTask()
    verify(log).info("Game not scheduled. Start game manually on : POST -> http://localhost:8080/dom/game/start-new")
  }
}
