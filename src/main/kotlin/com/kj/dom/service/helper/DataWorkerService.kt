package com.kj.dom.service.helper

import com.kj.dom.model.AdMessage
import com.kj.dom.model.AdProbability
import java.io.File
import java.util.UUID
import kotlin.random.Random

data class AdLog(
    val gameId: String,
    val move: String,
    val turn: Int,
    val gameTurnId: String,
    val adId: String,
    val message: String,
    val expiresIn: Int,
    val probability: String
)

fun main() {
//    consolidateProbabilities()
    calculateRewardWorth()
}

fun consolidateProbabilities() {
    val item = "wingpotmax"
    val file = File("logs/${item}_item_turns.csv")
    val lines = file.readLines().drop(1)

    val logs = lines.map { line ->
        val parts = line.split(",").map { it.trim('"') }
        AdLog(
            gameId = parts[0],
            move = parts[1],
            turn = parts[2].toInt(),
            gameTurnId = parts[3],
            adId = parts[4],
            message = parts[5],
            expiresIn = parts[6].toInt(),
            probability = parts[7]
        )
    }

    val logsByGame = logs.groupBy { it.gameId }
    val output = StringBuilder()

    logsByGame.forEach { (gameId, entries) ->
        val sortedTurns = entries.groupBy { it.turn }

        sortedTurns.forEach { (turn, turnAds) ->
            if (turnAds.any { it.move == "buy" }) {
                val nextTurnAds = sortedTurns[turn + 1] ?: return@forEach

                val buyAdMap = turnAds.associateBy { it.adId }
                val nextAdMap = nextTurnAds.associateBy { it.adId }

                val changes = nextAdMap.mapNotNull { (adId, nextAd) ->
                    val currentAd = buyAdMap[adId]
                    if (currentAd != null && currentAd.probability != nextAd.probability) {
                        "{turn: ${currentAd.turn}, probability: ${currentAd.probability}} -> " +
                                "{turn: ${nextAd.turn}, probability: ${nextAd.probability}}"
                    } else null
                }

                if (changes.isNotEmpty()) {
                    output.appendLine("Game: $gameId | Turn $turn (buy) -> ${turn + 1}")
                    changes.forEach { output.appendLine("  $it") }
                    output.appendLine()
                }
            }
        }
    }

    File("logs/probability_changes_by_game_${item}.txt").writeText(output.toString())
    println("Exported changes to logs/probability_changes_by_game_${item}.txt")
}

fun calculateRewardWorth() {
    val ads = generateRandomAds()
    val worthMap: Map<AdMessage, Double> = ads.associateWith { calculateAdRewardWorth(it) }

    worthMap.entries
        .sortedByDescending { it.value }
        .forEach { (ad, worth) ->
            println("Ad: ${ad.adId.take(6)} | Probability: ${ad.probability} | Reward: ${ad.reward} | Worth Score: ${"%.2f".format(worth)}")
        }
}

fun generateRandomAds(): List<AdMessage> {
    return List(20) {
        val prob = AdProbability.entries.random()
        val reward = Random.nextInt(5, 100).toString()

        AdMessage(
            adId = UUID.randomUUID().toString(),
            message = "message ${it + 1}",
            reward = reward,
            expiresIn = 7,
            probability = prob.displayName
        )
    }
}

fun calculateAdRewardWorth(ad: AdMessage): Double {
    val prob = AdProbability.entries.find { it.displayName == ad.probability } ?: return 0.0
    return (prob.successPct / 100.0) * ad.reward.toInt()
}