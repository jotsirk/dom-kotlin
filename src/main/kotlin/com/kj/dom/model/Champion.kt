package com.kj.dom.model

data class Champion(
  var gameState: GameState,
  var isGameRunning: Boolean = false,
  val items: MutableList<String> = mutableListOf(),
  val moves: MutableList<SuggestedMove> = mutableListOf(),
  val moveLogs: MutableList<MoveLog> = mutableListOf(),
  var boughtQuestItemPreviousTurn: Boolean = false,
)

data class MoveLog(
  var turn: Int,
  var move: SuggestedMove,
  var adsWorthMap: Map<AdMessage, Double>,
  var goldTuringMove: Int,
  var result: String? = null,
)
