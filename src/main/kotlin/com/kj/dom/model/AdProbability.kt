package com.kj.dom.model

enum class AdProbability(val displayName: String, val percentage: Int) {

    SURE_THING("Sure thing", 97),
    PIECE_OF_CAKE("Piece of cake", 92),
    WALK_IN_THE_PARK("Walk in the park", 84),
    QUITE_LIKELY("Quite likely", 74),
    HMMM("Hmmm....",63),
    GAMBLE("Gamble", 54),
    RISKY("Risky",42),
    RATHER_DETRIMENTAL("Rather detrimental", 36),
    SUICIDE_MISSION("Suicide mission", 10),
    PLAYING_WITH_FIRE("Playing with fire", 0),
    IMPOSSIBLE("Impossible", 0);

    companion object {
        private val displayNameMap = entries.associateBy { it.displayName }

        fun fromDisplayName(displayName: String): AdProbability? {
            return displayNameMap[displayName]
        }
    }
}
