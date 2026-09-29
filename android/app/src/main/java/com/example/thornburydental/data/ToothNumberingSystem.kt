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
    /** Universal permanent dentition (1-32) and primary lettered dentition (A-T). */
    UNIVERSAL("Universal", "1 to 32 (Adult), A to T (Pediatric)"),

    /** FDI two-digit: permanent 11-48, primary 51-85. */
    FDI("FDI two-digit", "11 to 48 (Permanent), 51 to 85 (Primary)");

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

        fun isPrimaryFdi(fdi: Int): Boolean {
            val quadrant = fdi / 10
            val position = fdi % 10
            return quadrant in 5..8 && position in 1..5
        }

        fun isPermanentFdi(fdi: Int): Boolean {
            val quadrant = fdi / 10
            val position = fdi % 10
            return quadrant in 1..4 && position in 1..8
        }

        // Primary Universal Letter <-> FDI mapping
        private val UNIVERSAL_LETTER_TO_FDI = mapOf(
            'A' to 55, 'B' to 54, 'C' to 53, 'D' to 52, 'E' to 51,
            'F' to 61, 'G' to 62, 'H' to 63, 'I' to 64, 'J' to 65,
            'K' to 75, 'L' to 74, 'M' to 73, 'N' to 72, 'O' to 71,
            'P' to 81, 'Q' to 82, 'R' to 83, 'S' to 84, 'T' to 85
        )

        private val FDI_TO_UNIVERSAL_LETTER = UNIVERSAL_LETTER_TO_FDI.entries.associate { (k, v) -> v to k }

        // Permanent Universal Number <-> FDI mapping
        private val UNIVERSAL_NUM_TO_FDI = mapOf(
            1 to 18, 2 to 17, 3 to 16, 4 to 15, 5 to 14, 6 to 13, 7 to 12, 8 to 11,
            9 to 21, 10 to 22, 11 to 23, 12 to 24, 13 to 25, 14 to 26, 15 to 27, 16 to 28,
            17 to 38, 18 to 37, 19 to 36, 20 to 35, 21 to 34, 22 to 33, 23 to 32, 24 to 31,
            25 to 41, 26 to 42, 27 to 43, 28 to 44, 29 to 45, 30 to 46, 31 to 47, 32 to 48
        )

        private val FDI_TO_UNIVERSAL_NUM = UNIVERSAL_NUM_TO_FDI.entries.associate { (k, v) -> v to k }

        fun universalLetterToPrimaryFdi(letter: Char): Int? =
            UNIVERSAL_LETTER_TO_FDI[letter.uppercaseChar()]

        fun primaryFdiToUniversalLetter(fdi: Int): Char? =
            FDI_TO_UNIVERSAL_LETTER[fdi]

        fun universalNumberToPermanentFdi(universal: Int): Int? =
            UNIVERSAL_NUM_TO_FDI[universal]

        fun permanentFdiToUniversalNumber(fdi: Int): Int? =
            FDI_TO_UNIVERSAL_NUM[fdi]

        /**
         * Returns the permanent successor FDI number for a given primary tooth FDI number.
         * Primary incisors & canines succeed to permanent incisors & canines.
         * Primary molars succeed to permanent premolars.
         */
        fun getPermanentSuccessorFdi(primaryFdi: Int): Int? {
            val quad = primaryFdi / 10
            val pos = primaryFdi % 10
            val permanentQuad = when (quad) {
                5 -> 1
                6 -> 2
                7 -> 3
                8 -> 4
                else -> return null
            }
            val permanentPos = when (pos) {
                1, 2, 3 -> pos           // Central Incisor, Lateral Incisor, Canine
                4 -> 4                   // 1st Primary Molar -> 1st Premolar
                5 -> 5                   // 2nd Primary Molar -> 2nd Premolar
                else -> return null
            }
            return permanentQuad * 10 + permanentPos
        }
    }
}
