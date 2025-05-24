package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability

object GameUtil {

    fun createAdProbabilityMap(ads: List<AdMessage>): Map<String, List<AdMessage>> {
        val easyAds = ads.filter { AdProbability.EASY.contains(AdProbability.fromDisplayName(it.probability)) }
        val mediumAds = ads.filter { AdProbability.MEDIUM.contains(AdProbability.fromDisplayName(it.probability)) }
        val hardAds = ads.filter { AdProbability.HARD.contains(AdProbability.fromDisplayName(it.probability)) }

        return mapOf(
            ProbabilityLevel.EASY.name to easyAds,
            ProbabilityLevel.MEDIUM.name to mediumAds,
            ProbabilityLevel.HARD.name to hardAds
        )
    }

    fun calculateAdRewardWorth(ad: AdMessage): Double {
        val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
        return (prob.successPct / 100.0) * ad.reward.toInt()
    }
}