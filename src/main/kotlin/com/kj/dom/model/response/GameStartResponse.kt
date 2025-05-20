package com.kj.dom.model.response

import com.kj.dom.model.GameState

data class GameStartResponse(
    val gameId: String,
    val lives: Int,
    val gold: Int,
    val level: Int,
    val score: Int,
    val highScore: Int,
    val turn: Int
) {

    fun toModel(): GameState = GameState(
        gameId = gameId,
        lives = lives,
        gold = gold,
        score = score,
        highScore = highScore,
        turn = turn
    )
}