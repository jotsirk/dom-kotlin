package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import com.kj.dom.model.Champion
import com.kj.dom.model.ProbabilityLevel
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.HARD
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.SuggestedMove
import com.kj.dom.strategy.CriticalHealthStrategy
import com.kj.dom.strategy.NormalHealthStrategy
import java.util.Base64

object GameUtil {
  fun groupAdsByProbabilityLevel(ads: List<AdMessage>): Map<ProbabilityLevel, List<AdMessage>> {
    val easyAds = ads.filter { AdProbability.EASY.contains(AdProbability.fromDisplayName(it.probability)) }
    val mediumAds = ads.filter { AdProbability.MEDIUM.contains(AdProbability.fromDisplayName(it.probability)) }
    val hardAds = ads.filter { AdProbability.HARD.contains(AdProbability.fromDisplayName(it.probability)) }

    return mapOf(
      EASY to easyAds,
      MEDIUM to mediumAds,
      HARD to hardAds,
    )
  }

  fun calculateAdRewardWorth(ad: AdMessage): Double {
    val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
    return (prob.successPct / 100.0) * ad.reward
  }

  fun decodeBase64(encoded: String): String {
    return String(Base64.getDecoder().decode(encoded))
  }

  fun calculateSuggestedMove(
    ads: List<AdMessage>,
    champion: Champion,
  ): SuggestedMove {
    val strategy =
      if (champion.gameState.lives == 1) {
        CriticalHealthStrategy()
      } else {
        NormalHealthStrategy()
      }

    return strategy.suggestMove(champion, ads)
  }

}
