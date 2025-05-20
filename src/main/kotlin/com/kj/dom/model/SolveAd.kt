package com.kj.dom.model

data class SolveAd(
    val success: Boolean,
    val lives: Int,
    val gold: Int,
    val score: Int,
    val highScore: Int,
    val turn: Int,
    val message: String,
)