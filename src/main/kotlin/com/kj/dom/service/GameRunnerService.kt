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
import org.springframework.stereotype.Service

@Service
class GameRunnerService {

    @Autowired
    private lateinit var log: Logger

    @Autowired
    private lateinit var gameService: GameService

    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val taskMap = ConcurrentHashMap<String, Future<Champion>>()

    fun executeGameTask(): String {
        val champion = Champion(gameState = gameService.startGame())
        val taskId = UUID.randomUUID().toString()

        val future = executor.submit(Callable {
            try {
                gameService.solveGame(champion)
            } catch (e: Exception) {
                log.error("Error solving game", e)
                throw e
            }
        })

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

    fun stopAll() {
        executor.shutdownNow()
    }
}