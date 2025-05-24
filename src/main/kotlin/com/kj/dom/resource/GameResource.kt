package com.kj.dom.resource

import com.kj.dom.service.GameRunnerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
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
}