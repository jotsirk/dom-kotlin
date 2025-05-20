package com.kj.dom.model

data class GameState(
    val gameId: String,
    val lives: Int,
    val gold: Int,
    val score: Int,
    val highScore: Int,
    val turn: Int,
)
