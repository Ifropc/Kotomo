package net.kanjitomo

import java.awt.Rectangle

/**
 * OCR results for a single target character
 */
class IdentifiedCharacter     // TODO normalized scores
    (
    /**
     * List of reference characters that match the target character best,
     * ordered by OCR score (first character is the closest match).
     */
    val referenceCharacters: String,
    /**
     * Location of the character in target image's coordinates
     */
    val location: Rectangle,
    /**
     * OCR scores for each reference character. Same order as in referenceCharacters.
     * Higher score is better but reference characters might have been re-ordered if
     * first match didn't result in a valid dictionary word.
     */
    val scores: List<Int>
)
