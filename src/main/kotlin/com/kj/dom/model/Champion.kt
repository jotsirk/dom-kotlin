package com.kj.dom.model

data class Champion(
    var gameState: GameState,
    var isGameRunning: Boolean = false,
    val items: MutableList<String> = mutableListOf(),
    val moves: MutableList<SuggestedMove> = mutableListOf(),
)
