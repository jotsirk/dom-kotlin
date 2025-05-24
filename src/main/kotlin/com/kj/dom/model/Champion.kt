package com.kj.dom.model

data class Champion(
    var gameState: GameState,
    var isGameRunning: Boolean = false,
    val items: List<ShopItemEnum> = mutableListOf(),
    val moves: List<SuggestedMove> = mutableListOf(),
)
