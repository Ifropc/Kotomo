/*
 * Copyright 2022 Ifropc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.github.ifropc.kotomo

import io.github.ifropc.kotomo.util.CharacterUtil
import kotlinx.serialization.Serializable

/**
 * Single Japanese word loaded from Jim Breen's EDICT dictionary.
 */
@Serializable
class Word  {
    /**
     * Word in kanji form (might also contain kana characters)
     */

    var kanji: String? = null

    /**
     * Word in kana form
     */

    var kana: String? = null

    /**
     * English description
     */

    var description: String? = null

    /**
     * If true, this is a common word.
     */

    var common = false

    /**
     * If true, this word is from names dictionary.
     * If false, this word is from default dictionary.
     */

    var name = false

    /**
     * Number of kanji characters in the kanji field
     */

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
}
