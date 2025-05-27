package com.kj.dom.resource

import com.kj.dom.model.Champion
import com.kj.dom.service.GameRunnerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dom/game")
class GameResource {
  @Autowired
  private lateinit var gameRunnerService: GameRunnerService

  @PostMapping("start-new")
  fun startNewGame(): ResponseEntity<Void> {
    gameRunnerService.executeGameTask()
    return ResponseEntity.ok().build()
  }

  @GetMapping("all-task-ids")
  fun getAllTaskIds(): ResponseEntity<List<String>> {
    return ResponseEntity.ok(gameRunnerService.getAllTaskIds())
  }

  @GetMapping("game-result")
  fun getGameResult(@RequestParam taskId: String): ResponseEntity<Champion> {
    val result = gameRunnerService.getGameResult(taskId)
    return if (result != null) {
      ResponseEntity.ok(result)
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
