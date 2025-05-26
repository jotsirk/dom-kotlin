package com.kj.dom.service.helper

import com.kj.dom.DomData.adsList1
import com.kj.dom.model.GameState
import org.junit.jupiter.api.Test

class GameUtilTest {
  @Test
  fun `calculateSuggestedMove - -`() {
    // given
    val gameState =
      GameState(
        gameId = "01JW6GXWGTJTCMKA29WTYK9SBX",
        lives = 5,
        gold = 1300,
        turn = 35,
        score = 1300,
        highScore = 1300,
      )
    val ads = adsList1

    // when
//    val result = GameUtil.calculateSuggestedMove(ads, gameState)

    // then
//    println(result)
  }
}
