package net.kanjitomo

import java.awt.Rectangle

/**
 * Results of the OCR run
 */
class OCRResults(
    characters: List<String>, locations: List<Rectangle>,
    scores: List<List<Int>>, words: List<Word>?, searchString: String?, vertical: Boolean
) {
    /**
     * String of best matches starting from OCR target point.
     */
    @JvmField
    val bestMatchingCharacters: String

    /**
     * List of characters identified by OCR algorithm. First character in each element
     * is added to bestMatchingCharacters.
     */
    val characters: MutableList<IdentifiedCharacter>

    /**
     * String used in dictionary search
     */
    val searchString: String?

    /**
     * Results of dictionary search from best matching characters.
     * Sorted roughly by ascending length.
     */
    val words: List<Word>?

    /**
     * If true, vertical orientation was used as reading direction.
     * If false, horizontal orientation was used.
     */
    val vertical: Boolean

    init {
        this.characters = ArrayList()
        var bestMatchingCharacters = ""
        for (i in characters.indices) {
            val character = IdentifiedCharacter(characters[i], locations[i], scores[i])
            bestMatchingCharacters += character.referenceCharacters[0]
            this.characters.add(character)
        }
        this.bestMatchingCharacters = bestMatchingCharacters
        this.words = words
        this.searchString = searchString
        this.vertical = vertical
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("\nCharacters:\n")
        for (character in characters) {
            sb.append(
                """
    ${character.referenceCharacters}
    
    """.trimIndent()
            )
        }
        sb.append("\nLocations:\n")
        for (character in characters) {
            sb.append(
                """
    ${character.location}
    
    """.trimIndent()
            )
        }
        if (searchString != null) {
            sb.append("\nSearch string:$searchString\n")
            sb.append(
                """
    Words found:${words!!.size}
    
    """.trimIndent()
            )
            sb.append("First word:")
            if (words.size > 0) {
                sb.append(words[0])
            }
        }
        return sb.toString()
    }
}
