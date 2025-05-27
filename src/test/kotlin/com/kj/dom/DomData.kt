package com.kj.dom

import com.kj.dom.model.AdMessage
import com.kj.dom.model.Champion
import com.kj.dom.model.GameState

object DomData {
  val walkInTheParkAd1 =
    AdMessage(
      adId = "upgJgQHe",
      message = "Help Iudicael Gilbert to fix their chariot",
      reward = 2,
      expiresIn = 7,
      probability = "Walk in the park",
    )

  val ratherDetrimentalAd1 =
    AdMessage(
      adId = "dEfuNMfy",
      message = "Create an advertisement campaign for Iohannes Millward to promote their cat based business",
      reward = 24,
      expiresIn = 7,
      probability = "Rather detrimental",
    )

  val walkInTheParkAd2 =
    AdMessage(
      adId = "00XfuckI",
      message = "Help Arax Christopherson to transport a magic beer mug to village in Orrincrest",
      reward = 19,
      expiresIn = 7,
      probability = "Walk in the park",
    )

  val walkInTheParkAd3 =
    AdMessage(
      adId = "09PDX9O0",
      message = "Help Ale Nicholson to transport a magic chicken to steppe in Southfire",
      reward = 18,
      expiresIn = 7,
      probability = "Walk in the park",
    )

  val hmmmAd1 =
    AdMessage(
      adId = "gV7yVtTN",
      message = "Escort Kawacatoose Christians to steppe in Thornham where they can meet with their long lost beer mug",
      reward = 72,
      expiresIn = 7,
      probability = "Hmmm....",
    )

  val walkInTheParkAd4 =
    AdMessage(
      adId = "ShQlro8c",
      message = "Help Juhan Washington to reach an agreement with Bobbi Outterridge on the matters of disputed dog",
      reward = 9,
      expiresIn = 7,
      probability = "Walk in the park",
    )

  val pieceOfCakeAd1 =
    AdMessage(
      adId = "EJwEZlD8",
      message = "Create an advertisement campaign for Lúcia Forrest to promote their cat based business",
      reward = 45,
      expiresIn = 7,
      probability = "Piece of cake",
    )

  val riskyAd1 =
    AdMessage(
      adId = "2rdWQg8Z",
      message = "Create an advertisement campaign for Salacia Hunnisett to promote their pot based business",
      reward = 26,
      expiresIn = 7,
      probability = "Risky",
    )

  val pieceOfCakeAd2 =
    AdMessage(
      adId = "A6qGd7DQ",
      message = "Help Cesarino Jernigan to sell an unordinary chicken on the local market",
      reward = 32,
      expiresIn = 7,
      probability = "Piece of cake",
    )

  val quiteLikelyAd1 =
    AdMessage(
      adId = "9hNd8V4C",
      message = "Escort Firuz Whinery to savannah in Westville where they can meet with their long lost potatoes",
      reward = 71,
      expiresIn = 7,
      probability = "Quite likely",
    )


  val pieceOfCakeAd3 = AdMessage(
    adId = "vQBMExdF",
    message = "Help defending thrift shoppe in Thorncrest from the intruders",
    reward = 77,
    expiresIn = 2,
    encrypted = null,
    probability = "Piece of cake"
  )

  val riskyAd2 = AdMessage(
    adId = "ZHbEveXw",
    message = "Investigate Epiphanius Odson and find out their relation to the magic pan.",
    reward = 126,
    expiresIn = 1,
    encrypted = 1,
    probability = "Risky"
  )

  val playingWithFireAd1 = AdMessage(
    adId = "nCyFzlzw",
    message = "Infiltrate The Invisibles and recover their secrets.",
    reward = 167,
    expiresIn = 1,
    encrypted = 1,
    probability = "Playing with fire"
  )

  val suicideMissionAd1 = AdMessage(
    adId = "w76w9zZt",
    message = "Infiltrate The Crystal Dragontooth Association and recover their secrets.",
    reward = 179,
    expiresIn = 1,
    encrypted = 1,
    probability = "Suicide mission"
  )

  val ratherDetrimentalAd2 = AdMessage(
    adId = "9EGzrpFQ",
    message = "Infiltrate The Ebony Crosses and recover their secrets.",
    reward = 158,
    expiresIn = 1,
    encrypted = 1,
    probability = "Rather detrimental"
  )

  val hmmmAd2 = AdMessage(
    adId = "vklJGeMT",
    message = "Investigate Rolf Allaire and find out their relation to the magic dog.",
    reward = 148,
    expiresIn = 2,
    encrypted = 1,
    probability = "Hmmm...."
  )

  val ratherDetrimentalAd3 = AdMessage(
    adId = "VV2tIqj5",
    message = "Investigate Gabby Bragason and find out their relation to the magic wagon.",
    reward = 172,
    expiresIn = 2,
    encrypted = 1,
    probability = "Rather detrimental"
  )

  val hmmmAd3 = AdMessage(
    adId = "vpaCEZdR",
    message = "Help defending thrift shoppe in Scalehill from the intruders",
    reward = 95,
    expiresIn = 7,
    encrypted = null,
    probability = "Hmmm...."
  )

  val hmmmAd4 = AdMessage(
    adId = "wsuUTbcP",
    message = "Help defending village in Snowwell from the intruders",
    reward = 98,
    expiresIn = 7,
    encrypted = null,
    probability = "Hmmm...."
  )

  val quiteLikelyAd2 = AdMessage(
    adId = "iCxzACpG",
    message = "Help defending thrift shoppe in Newmoore from the intruders",
    reward = 119,
    expiresIn = 7,
    encrypted = null,
    probability = "Quite likely"
  )

  val adsList1 = listOf(
    pieceOfCakeAd3,
    riskyAd2,
    playingWithFireAd1,
    suicideMissionAd1,
    ratherDetrimentalAd2,
    hmmmAd2,
    ratherDetrimentalAd3,
    hmmmAd3,
    hmmmAd4,
    quiteLikelyAd2,
  )

  val referenceGameState =
    GameState(gameId = "01JW7A7TM0N5TKRJQG8EJJX5YV", lives = 3, gold = 0, score = 0, turn = 0, highScore = 0)

  val referenceChampion = Champion(
    gameState = referenceGameState,
    isGameRunning = false,
    items = mutableListOf(),
    moves = mutableListOf(),
  )
}
