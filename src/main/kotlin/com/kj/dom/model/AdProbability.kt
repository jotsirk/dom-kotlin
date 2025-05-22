package com.kj.dom.model

enum class AdProbability(val displayName: String, val percentage: Int, val acceptableValue: Int) {

    SURE_THING("Sure thing", 97, 25),
    PIECE_OF_CAKE("Piece of cake", 92, 20),
    WALK_IN_THE_PARK("Walk in the park", 84, 30),
    QUITE_LIKELY("Quite likely", 74, 35),
    HMMM("Hmmm....",63, 60),
    GAMBLE("Gamble", 54, 50),
    RISKY("Risky",42, 60),
    RATHER_DETRIMENTAL("Rather detrimental", 36, 60),
    SUICIDE_MISSION("Suicide mission", 10, 60),
    PLAYING_WITH_FIRE("Playing with fire", 0, 60),
    IMPOSSIBLE("Impossible", 0, 60);

    companion object {
        private val displayNameMap = entries.associateBy { it.displayName }

        fun fromDisplayName(displayName: String): AdProbability? {
            return displayNameMap[displayName]
        }
    }
}
