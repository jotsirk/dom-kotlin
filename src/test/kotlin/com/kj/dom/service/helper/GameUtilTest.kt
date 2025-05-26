package com.kj.dom.service.helper

import com.kj.dom.DomData.adsList1
import com.kj.dom.DomData.hmmmAd2
import com.kj.dom.DomData.hmmmAd3
import com.kj.dom.DomData.hmmmAd4
import com.kj.dom.DomData.pieceOfCakeAd2
import com.kj.dom.DomData.pieceOfCakeAd3
import com.kj.dom.DomData.playingWithFireAd1
import com.kj.dom.DomData.quiteLikelyAd2
import com.kj.dom.DomData.ratherDetrimentalAd2
import com.kj.dom.DomData.ratherDetrimentalAd3
import com.kj.dom.DomData.referenceChampion
import com.kj.dom.DomData.referenceGameState
import com.kj.dom.DomData.riskyAd2
import com.kj.dom.DomData.suicideMissionAd1
import com.kj.dom.model.MoveType.BUY
import com.kj.dom.model.MoveType.SOLVE
import com.kj.dom.model.MoveType.SOLVE_MULTIPLE
import com.kj.dom.model.MoveType.WAIT
import com.kj.dom.model.ProbabilityLevel.EASY
import com.kj.dom.model.ProbabilityLevel.HARD
import com.kj.dom.model.ProbabilityLevel.MEDIUM
import com.kj.dom.model.ShopItemEnum.HPOT
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

  @Test
  fun `calculateSuggestedMove - returns immediate hpot buy move - if 1 life and over 50 gold`() {
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
  fun `calculateSuggestedMove - returns easiest solve move - if 1 life and under 50 gold`() {
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
  fun `calculateSuggestedMove - returns multi ad solve move - if 1 life and under 49 gold and multiple easy ads amount to gold needed`() {
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
  fun `calculateSuggestedMove - returns wait solve move - if 1 life and more than 2 ads expire next turn`() {
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
  fun `calculateSuggestedMove - returns solve move for highest reward easy ad - if 1 life and no valid ads found or multi valid ads`() {
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
  fun `calculateSuggestedMove - returns solve move for highest accept to reward ad - if 1 life and no valid ads found before`() {
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
}
