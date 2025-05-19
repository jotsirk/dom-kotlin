package com.kj.dom.model.response

data class GameStartResponse(
    val gameId: String,
    val lives: Int,
    val gold: Int,
    val level: Int,
    val score: Int,
    val highScore: Int,
    val turn: Int
)