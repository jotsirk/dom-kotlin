package com.kj.dom.service

import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class GameInitializerService(
    @Value("\${dom.start.manually:false}")
    private val startManually: Boolean
) : ApplicationRunner {

    @Autowired
    private lateinit var gameRunnerService: GameRunnerService

    @Autowired
    private lateinit var log: Logger

    override fun run(args: ApplicationArguments?) {
        if (!startManually) {
            gameRunnerService.executeGameTask()
        } else {
            log.info("Game not. Start game manually on : -endpoint-")
        }
    }
}