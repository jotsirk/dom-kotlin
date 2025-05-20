package com.kj.dom.model.response

import com.kj.dom.model.SolveAd

data class SolveAdResponse(
    val success: Boolean,
    val lives: Int,
    val gold: Int,
    val score: Int,
    val highScore: Int,
    val turn: Int,
    val message: String,
) {
    fun toModel() = SolveAd(
        success = success,
        lives = lives,
        gold = gold,
        score = score,
        highScore = highScore,
        turn = turn,
        message = message,
    )
}
