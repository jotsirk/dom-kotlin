package com.kj.dom.strategy

import com.kj.dom.model.AdMessage
import com.kj.dom.model.Champion
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.SuggestedMove

interface HealthStrategy {
  fun suggestMove(
    champion: Champion,
    ads: List<AdMessage>,
  ): SuggestedMove

  fun Champion.canAffordHealthPotion() = this.gameState.gold >= HPOT.price

  fun List<AdMessage>.filterOutStealAds() = this.filterNot { it.message.contains(AD_MESSAGE_STEAL, true) }

  private companion object {
    const val AD_MESSAGE_STEAL = "steal"
  }
}
