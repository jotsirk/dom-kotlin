package com.kj.dom.model

enum class AdProbability(
  val displayName: String,
  val successPct: Int,
  val acceptableValue: Int,
) {
  SURE_THING("Sure thing", 97, 15),
  PIECE_OF_CAKE("Piece of cake", 92, 18),
  WALK_IN_THE_PARK("Walk in the park", 84, 45),
  QUITE_LIKELY("Quite likely", 74, 55),
  HMMM("Hmmm....", 63, 80),
  GAMBLE("Gamble", 54, 85),
  RISKY("Risky", 42, 90),
  RATHER_DETRIMENTAL("Rather detrimental", 36, 150),
  SUICIDE_MISSION("Suicide mission", 10, 200),
  PLAYING_WITH_FIRE("Playing with fire", 0, 1000),
  IMPOSSIBLE("Impossible", 0, 1000),
  ;

  companion object {
    private val displayNameMap = entries.associateBy { it.displayName }

    val EASY = listOf(SURE_THING, PIECE_OF_CAKE, WALK_IN_THE_PARK)
    val MEDIUM = listOf(QUITE_LIKELY, HMMM, GAMBLE)
    val HARD = listOf(RISKY, RATHER_DETRIMENTAL, SUICIDE_MISSION, PLAYING_WITH_FIRE, IMPOSSIBLE)

    fun fromDisplayName(displayName: String): AdProbability? = displayNameMap[displayName]
  }
}
