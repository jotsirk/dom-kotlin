package com.kj.dom.service.helper

import com.kj.dom.DomData.adsList1
import com.kj.dom.DomData.hmmmAd1
import com.kj.dom.DomData.hmmmAd2
import com.kj.dom.DomData.hmmmAd3
import com.kj.dom.DomData.hmmmAd4
import com.kj.dom.DomData.pieceOfCakeAd1
import com.kj.dom.DomData.pieceOfCakeAd2
import com.kj.dom.DomData.pieceOfCakeAd3
import com.kj.dom.DomData.playingWithFireAd1
import com.kj.dom.DomData.quiteLikelyAd2
import com.kj.dom.DomData.ratherDetrimentalAd2
import com.kj.dom.DomData.ratherDetrimentalAd3
import com.kj.dom.DomData.referenceChampion
import com.kj.dom.DomData.referenceGameState
import com.kj.dom.DomData.riskyAd1
import com.kj.dom.DomData.riskyAd2
import com.kj.dom.DomData.suicideMissionAd1
import com.kj.dom.DomData.walkInTheParkAd1
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.BUY_AND_SOLVE
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.HARD
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.ShopItemEnum.HPOT
import com.kj.dom.model.ShopItemEnum.WAX
import com.kj.dom.model.SuggestedMove
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

class GameUtilTest {
  @Test
  fun `groupAdsByProbabilityLevel - returns map of ads grouped by probability level - if no errors`() {
    // given
    val ads = adsList1

    // when
    val result = GameUtil.groupAdsByProbabilityLevel(ads)

    // then
    assertThat(result[EASY]).contains(pieceOfCakeAd3)
    assertThat(result[MEDIUM]).contains(hmmmAd2, hmmmAd3, hmmmAd4, quiteLikelyAd2)
    assertThat(result[HARD]).contains(
      riskyAd2,
      playingWithFireAd1,
      suicideMissionAd1,
      ratherDetrimentalAd2,
      ratherDetrimentalAd3,
    )
  }

  @Test
  fun `calculateAdRewardWorth - returns valid reward worth - if correct probability`() {
    // given
    val ad = hmmmAd2

    // when
    val result = GameUtil.calculateAdRewardWorth(ad)

    // then
    assertThat(result).isEqualTo(93.24)
  }

  @Test
  fun `calculateAdRewardWorth - returns 0 reward worth - if incorrect probability`() {
    // given
    val ad = hmmmAd2.copy(probability = "broken probability")

    // when
    val result = GameUtil.calculateAdRewardWorth(ad)

    // then
    assertThat(result).isEqualTo(0.0)
  }

  @Test
  fun `decodeBase64 - returns decoded string - if no errors`() {
    // given
    val encodedString = "U29tZSB0ZXh0IA=="

    // when
    val result = GameUtil.decodeBase64(encodedString)

    // then
    assertThat(result).isEqualTo("Some text ")
  }

  @Test
  fun `decodeBase64 - throws IllegalArgumentException - if string value is not base64`() {
    // given
    val encodedString = "Some text"

    // when
    val result = catchThrowable { GameUtil.decodeBase64(encodedString) }

    // then
    assertThat(result).isInstanceOfSatisfying(IllegalArgumentException::class.java) {
      assertThat(it.message).isEqualTo("Illegal base64 character 20")
    }
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns immediate hpot buy move - if CriticalHealthStrategy and 1 life and over 50 gold`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 51),
      )
    val ads = adsList1

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(BUY, itemId = HPOT.id))
  }

  @Test
  fun `calculateSuggestedMove - returns easiest solve move - if CriticalHealthStrategy and 1 life and under 50 gold`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 49),
      )
    val ads = adsList1

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(pieceOfCakeAd3.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns multi ad solve move - if CriticalHealthStrategy and 1 life and under 49 gold and multiple easy ads amount to gold needed`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 10),
      )
    val ads =
      listOf(
        pieceOfCakeAd3.copy(reward = 25),
        pieceOfCakeAd2.copy(reward = 25),
        playingWithFireAd1.copy(expiresIn = 7),
        suicideMissionAd1.copy(expiresIn = 7),
        ratherDetrimentalAd2.copy(expiresIn = 7),
        hmmmAd2.copy(expiresIn = 7),
        ratherDetrimentalAd3.copy(expiresIn = 7),
        hmmmAd3.copy(expiresIn = 7),
        hmmmAd4.copy(expiresIn = 7),
        quiteLikelyAd2.copy(expiresIn = 7),
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(
      SuggestedMove(
        SOLVE_MULTIPLE,
        adIds = listOf(pieceOfCakeAd3.adId, pieceOfCakeAd2.adId),
      ),
    )
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns wait solve move - if CriticalHealthStrategy and 1 life and more than 2 ads expire next turn`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 10),
      )
    val ads = listOf(
      pieceOfCakeAd3.copy(reward = 25),
      pieceOfCakeAd2.copy(reward = 25),
      playingWithFireAd1.copy(expiresIn = 1),
      suicideMissionAd1.copy(expiresIn = 1),
      ratherDetrimentalAd2.copy(expiresIn = 7),
      hmmmAd2.copy(expiresIn = 7),
      ratherDetrimentalAd3.copy(expiresIn = 7),
      hmmmAd3.copy(expiresIn = 7),
      hmmmAd4.copy(expiresIn = 7),
      quiteLikelyAd2.copy(expiresIn = 7),
    )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(WAIT))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns solve move for highest reward easy ad - if CriticalHealthStrategy and 1 life and no valid ads found or multi valid ads`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 10),
      )
    val ads = listOf(
      pieceOfCakeAd3.copy(reward = 25),
      pieceOfCakeAd2.copy(reward = 10),
      playingWithFireAd1.copy(expiresIn = 3),
      suicideMissionAd1.copy(expiresIn = 4),
      ratherDetrimentalAd2.copy(expiresIn = 7),
      hmmmAd2.copy(expiresIn = 7),
      ratherDetrimentalAd3.copy(expiresIn = 7),
      hmmmAd3.copy(expiresIn = 7),
      hmmmAd4.copy(expiresIn = 7),
      quiteLikelyAd2.copy(expiresIn = 7),
    )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(pieceOfCakeAd3.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns solve move for highest acceptable score to reward ad - if 1 life and no valid ads found before`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 10),
      )
    val ads = listOf(
      pieceOfCakeAd3.copy(reward = 10),
      pieceOfCakeAd2.copy(reward = 10),
      playingWithFireAd1.copy(expiresIn = 3),
      suicideMissionAd1.copy(expiresIn = 4),
      ratherDetrimentalAd2.copy(expiresIn = 7),
      hmmmAd2.copy(expiresIn = 7),
      ratherDetrimentalAd3.copy(expiresIn = 7),
      hmmmAd3.copy(expiresIn = 7),
      hmmmAd4.copy(expiresIn = 7),
      quiteLikelyAd2.copy(expiresIn = 7),
    )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(hmmmAd2.adId)))
  }

  @Test
  fun `calculateSuggestedMove - returns highest score move - if 1 life and no other acceptable ads found`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 1, gold = 10),
      )
    val ads =
      listOf(
        pieceOfCakeAd3.copy(reward = 10),
        pieceOfCakeAd2.copy(reward = 10),
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        ratherDetrimentalAd2.copy(reward = 10, expiresIn = 7),
        hmmmAd2.copy(reward = 10, expiresIn = 7),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
        hmmmAd3.copy(reward = 10, expiresIn = 7),
        hmmmAd4.copy(reward = 10, expiresIn = 7),
        quiteLikelyAd2.copy(reward = 10, expiresIn = 7),
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(ratherDetrimentalAd3.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns best acceptable ad solve move - if normalHealthStrategy and no errors`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 3),
      )
    val ads =
      listOf(
        pieceOfCakeAd3.copy(reward = 10),
        pieceOfCakeAd2.copy(reward = 10),
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        hmmmAd2.copy(reward = 10, expiresIn = 7),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
        hmmmAd4.copy(reward = 10, expiresIn = 7),
        quiteLikelyAd2.copy(reward = 10, expiresIn = 7),
        walkInTheParkAd1,
        pieceOfCakeAd1,
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(pieceOfCakeAd1.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns buy and solve move - if normalHealthStrategy and easy acceptable ad reward is more than 100 and gold is more than 100`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 3, gold = 100),
      )
    val ads =
      listOf(
        pieceOfCakeAd3.copy(reward = 10),
        pieceOfCakeAd2.copy(reward = 10),
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        hmmmAd2.copy(reward = 10, expiresIn = 7),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
        hmmmAd4.copy(reward = 10, expiresIn = 7),
        quiteLikelyAd2.copy(reward = 10, expiresIn = 7),
        walkInTheParkAd1.copy(reward = 132, expiresIn = 7),
        pieceOfCakeAd1,
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(BUY_AND_SOLVE, itemId = HPOT.id, adIds = listOf(walkInTheParkAd1.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns best solve move - if normalHealthStrategy and easy acceptable ad and gold is less than 50`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 3, gold = 47),
      )
    val ads =
      listOf(
        riskyAd1,
        hmmmAd1,
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        hmmmAd2.copy(reward = 87, expiresIn = 7),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
        hmmmAd4.copy(reward = 10, expiresIn = 7),
        quiteLikelyAd2.copy(reward = 10, expiresIn = 7),
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(SOLVE, adIds = listOf(hmmmAd2.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns buy and solve move - if normalHealthStrategy and no easy ads and gold more than 50`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 3, gold = 100),
      )
    val ads =
      listOf(
        riskyAd1,
        hmmmAd1,
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        hmmmAd2.copy(reward = 87, expiresIn = 7),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
        hmmmAd4.copy(reward = 10, expiresIn = 7),
        quiteLikelyAd2.copy(reward = 10, expiresIn = 7),
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(BUY_AND_SOLVE, itemId = WAX.id, adIds = listOf(hmmmAd2.adId)))
  }

  @Suppress("ktlint:standard:max-line-length")
  @Test
  fun `calculateSuggestedMove - returns solve move with highest worth ad - if normalHealthStrategy and no easy or medium ads and gold less than 50`() {
    // given
    val champion =
      referenceChampion.copy(
        gameState = referenceGameState.copy(lives = 3, gold = 101),
      )
    val ads =
      listOf(
        riskyAd1.copy(reward = 97, expiresIn = 7),
        playingWithFireAd1.copy(reward = 10, expiresIn = 3),
        suicideMissionAd1.copy(reward = 10, expiresIn = 4),
        ratherDetrimentalAd3.copy(reward = 70, expiresIn = 7),
      )

    // when
    val result = GameUtil.calculateSuggestedMove(ads, champion)

    // then
    assertThat(result).isEqualTo(SuggestedMove(BUY_AND_SOLVE, itemId = HPOT.id, adIds = listOf(riskyAd1.adId)))
  }
}
