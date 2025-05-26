package com.kj.dom.service

import com.kj.dom.model.Champion
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.GONE
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class GameRunnerService {
  @Autowired
  private lateinit var log: Logger

  @Autowired
  private lateinit var gameService: GameService

  private val executor: ExecutorService = Executors.newFixedThreadPool(4)
  private val taskMap = ConcurrentHashMap<String, Future<Champion?>>()

  fun executeGameTask(): String {
    val taskId = UUID.randomUUID().toString()

    val future =
      executor.submit(
        Callable {
          return@Callable try {
            val gameState = gameService.startGame()

            log.info("Started game with id: ${gameState.gameId} and taskId: $taskId")

            val champion = Champion(gameState)

            gameService.solveGame(champion)
            champion
          } catch (e: HttpClientErrorException) {
            when (e.statusCode) {
              GONE -> log.error("Illegal move made - Game is over")
              else -> log.error("HTTP client error", e)
            }
            null
          } catch (e: Exception) {
            log.error("Error solving game", e)
            null
          }
        },
      )

    taskMap[taskId] = future
    return taskId
  }

  fun getTaskState(taskId: String): Champion? {
    val future = taskMap[taskId] ?: return null
    return if (future.isDone) future.get() else null
  }

  fun isRunning(taskId: String): Boolean {
    val future = taskMap[taskId] ?: return false
    return !future.isDone
  }

  fun getAllTaskIds(): List<String> {
    return taskMap.keys.toList()
  }

  fun getGameResult(taskId: String): Champion? {
    val future = taskMap[taskId] ?: return null

    return if (future.isDone && !future.isCancelled) {
      try {
        future.get()
      } catch (e: Exception) {
        log.error("Error retrieving task result", e)
        null
      }
    } else {
      null
    }
  }

  fun stopAll() {
    executor.shutdownNow()
  }
}
