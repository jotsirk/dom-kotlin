package com.kj.dom.model.response

import com.kj.dom.model.ShopItem

data class ShopResponse(
    val id: String,
    val name: String,
    val cost: Int,
) {
    fun toModel(): ShopItem = ShopItem(
        id = id,
        name = name,
        cost = cost,
    )
}
