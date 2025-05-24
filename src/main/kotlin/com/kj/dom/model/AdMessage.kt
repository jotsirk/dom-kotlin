package com.kj.dom.model

data class AdMessage(
    val adId: String = "",
    val message: String = "",
    val reward: String = "",
    val expiresIn: Int = 0,
    val encrypted: Int? = null,
    val probability: String = ""
)
