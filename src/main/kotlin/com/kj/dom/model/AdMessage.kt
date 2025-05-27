package com.kj.dom.model

data class AdMessage(
    val adId: String,
    val message: String,
    val reward: Int,
    val expiresIn: Int,
    val encrypted: Int? = null,
    val probability: String,
)
