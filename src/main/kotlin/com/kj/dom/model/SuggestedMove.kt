package com.kj.dom.model

data class SuggestedMove(
    val move: MoveType,
    val itemId: String? = null,
    val adIds: List<String> = emptyList(),
)