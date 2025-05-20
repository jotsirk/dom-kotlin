package com.kj.dom.model

data class AdMessage(
    val adId: String,
    val message: String,
    val reward: String,
    val expiresIn: Int,
    val encrypted: Any?,
    val probability: String,
)
