package com.kj.dom.service

import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.service.helper.DataMinerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service

@Service
class GameInitializerService: ApplicationRunner {

    @Autowired
    private lateinit var dataMinerService: DataMinerService

    @Autowired
    private lateinit var gameService: GameService

    override fun run(args: ApplicationArguments?) {
        dataMinerService.dataMine()
//        initializeGame()
    }

    // TODO maybe just init on postConstruct
    private fun initializeGame() {
        log.info("Initializing game")

        gameService.startGame()
        playGame()
    }

    private fun playGame() {
        // todo check game state (lives, money, etc)
        val gameSate = gameService.getGameState()

        log.debug("Game turn - %s".format(gameService.getGameState().turn))

        val shopItems = gameService.getShopItems(gameSate.gameId)

//        if (gameSate.lives == 1) {
//            val healthPotion = shopItems.items.find { it.name == HPOT.name }
//
//            if (healthPotion != null && healthPotion.cost <= gameSate.gold) {
//
//            }
//
//            // todo try to buy lives
//        }

        // todo check ads
        val messageBoardAds = gameService.getAdMessages(gameSate.gameId)


        // todo check shop possibility
        // todo calculate best possibilities run turn
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GameInitializerService::class.java)
    }
}