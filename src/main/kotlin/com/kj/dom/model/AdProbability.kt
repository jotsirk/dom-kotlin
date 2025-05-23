package com.kj.dom.model

enum class AdProbability(val displayName: String, val successPct: Int, val acceptableValue: Int) {

    SURE_THING("Sure thing", 97, 40),
    PIECE_OF_CAKE("Piece of cake", 92, 40),
    WALK_IN_THE_PARK("Walk in the park", 84, 50),
    QUITE_LIKELY("Quite likely", 74, 50),
    HMMM("Hmmm....",63, 60),
    GAMBLE("Gamble", 54, 70),
    RISKY("Risky",42, 80),
    RATHER_DETRIMENTAL("Rather detrimental", 36, 85),
    SUICIDE_MISSION("Suicide mission", 10, 90),
    PLAYING_WITH_FIRE("Playing with fire", 0, 100),
    IMPOSSIBLE("Impossible", 0, 100);

    companion object {
        private val displayNameMap = entries.associateBy { it.displayName }

        val EASY = listOf(SURE_THING, PIECE_OF_CAKE, WALK_IN_THE_PARK)
        val MEDIUM = listOf(QUITE_LIKELY, HMMM, GAMBLE, RISKY)
        val HARD = listOf(RATHER_DETRIMENTAL, SUICIDE_MISSION, PLAYING_WITH_FIRE, IMPOSSIBLE)

        fun fromDisplayName(displayName: String): AdProbability? {
            return displayNameMap[displayName]
        }
    }
}
