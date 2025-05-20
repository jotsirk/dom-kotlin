package com.kj.dom.model.response

import com.kj.dom.model.AdMessage

data class AdMessageResponse(
    val adId: String,
    val message: String,
    val reward: String,
    val expiresIn: Int,
    val encrypted: Any?,
    val probability: String,
) {
    fun toModel(): AdMessage = AdMessage(
        adId = adId,
        message = message,
        reward = reward,
        expiresIn = expiresIn,
        encrypted = encrypted,
        probability = probability,
    )
}
