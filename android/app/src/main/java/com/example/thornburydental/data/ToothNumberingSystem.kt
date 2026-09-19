package com.example.thornburydental.data

/**
 * Which tooth numbering scheme the clinic dictates in.
 *
 * This has to be stated, never guessed. The two schemes overlap: "14" is the upper-left first
 * molar in Universal and the upper-right first premolar in FDI - opposite sides of the mouth.
 * Accepting a number that is valid in either scheme and letting a lookup pick whichever matched
 * first is how a pocket depth ends up recorded against the wrong tooth.
 */
enum class ToothNumberingSystem(
    val displayName: String,
    val example: String
) {
    /** Universal permanent dentition, 1-32. Primary teeth are lettered, so not dictated numerically. */
    UNIVERSAL("Universal", "1 to 32"),

    /** FDI two-digit: permanent 11-48, primary 51-85. */
    FDI("FDI two-digit", "11 to 48, primary 51 to 85");

    fun isValidToothNumber(number: Int): Boolean = when (this) {
        UNIVERSAL -> number in 1..32
        FDI -> {
            val quadrant = number / 10
            val position = number % 10
            when (quadrant) {
                1, 2, 3, 4 -> position in 1..8   // permanent
                5, 6, 7, 8 -> position in 1..5   // primary
                else -> false
            }
        }
    }

    companion object {
        val DEFAULT = UNIVERSAL

        fun fromNameOrDefault(name: String?): ToothNumberingSystem =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
