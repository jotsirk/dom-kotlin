package com.kj.dom.model.response

data class AdMessageResponse(
    val adId: String,
    val message: String,
    val reward: String,
    val expiresIn: Int,
    val encrypted: Int?,
    val probability: String,
)
