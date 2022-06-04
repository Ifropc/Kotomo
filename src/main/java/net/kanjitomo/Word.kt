package net.kanjitomo

import net.kanjitomo.dictionary.CharacterUtil
import java.io.Serializable

/**
 * Single Japanese word loaded from Jim Breen's EDICT dictionary.
 */
class Word : Serializable {
    constructor() {
        // Kryo needs no-arg constructor
    }

    /**
     * Word in kanji form (might also contain kana characters)
     */
    @JvmField
    var kanji: String? = null

    /**
     * Word in kana form
     */
    @JvmField
    var kana: String? = null

    /**
     * English description
     */
    @JvmField
    var description: String? = null

    /**
     * If true, this is a common word.
     */
    @JvmField
    var common = false

    /**
     * If true, this word is from names dictionary.
     * If false, this word is from default dictionary.
     */
    @JvmField
    var name = false

    /**
     * Number of kanji characters in the kanji field
     */
    @JvmField
    var kanjiCount = 0

    /**
     * Creates a new word
     *
     * @param name If true, this word is from names dictionary. If false, this word
     * is from default dictionary.
     */
    constructor(kanji: String, kana: String?, description: String, name: Boolean) {
        this.kanji = kanji
        this.kana = kana
        this.description = description
        this.name = name
        common = if (description.contains("(P)")) {
            true
        } else {
            false
        }
        var kanjiCount = 0
        for (c in kanji.toCharArray()) {
            if (CharacterUtil.isKanji(c)) {
                ++kanjiCount
            }
        }
        this.kanjiCount = kanjiCount
    }

    override fun equals(obj: Any?): Boolean {
        val w = obj as Word?
        return kanji == w!!.kanji && kana == w.kana
    }

    override fun hashCode(): Int {
        return kanji.hashCode() + kana.hashCode()
    }

    override fun toString(): String {
        return "$kanji $kana"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
