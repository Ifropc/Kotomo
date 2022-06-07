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
package io.github.ifropc.kotomo.util

import java.util.*

/**
 * Utility methods for classifying characters
 */
object CharacterUtil {
    // https://stackoverflow.com/questions/19899554/unicode-range-for-japanese
    fun isHiragana(c: Char): Boolean {
        if (c == '｜') {
            return true
        }
        val codepoint = c.code
        return codepoint >= 0x3040 && codepoint <= 0x309F
    }

    fun isKatakana(c: Char): Boolean {
        if (c == '｜') {
            return true
        }
        val codepoint = c.code
        return codepoint >= 0x30A0 && codepoint <= 0x30FF
    }

    fun isKana(c: Char): Boolean {
        return isHiragana(c) || isKatakana(c)
    }

    fun isKanji(c: Char): Boolean {
        val codepoint = c.code
        return if (c.code == 0x3005) { // 々
            true
        } else codepoint >= 0x4E00 && codepoint <= 0x9FAF
    }

    /**
     * Converts katakana to hiragana.
     * Returns the same character if not katakana.
     */
    private fun toHiragana(c: Char): Char {

        // from http://en.wikipedia.org/wiki/Kana#Kana_in_Unicode
        if (!isKatakana(c)) return c
        var codepoint = c.code
        if (codepoint > 0x30F6) {
            return c
        }
        codepoint -= 0x60
        return codepoint.toChar()
    }

    /**
     * Converts katakana to hiragana, replaces synonyms and converts
     * to upper case (if alphabet).
     */
    fun toCanonical(str: String): String {
        val sb = StringBuilder()
        for (c in str.toCharArray()) {
            val converted = toCanonical(c)
            sb.append(converted)
        }
        return sb.toString().uppercase(Locale.getDefault())
    }

    /**
     * Converts katakana to hiragana, replaces synonyms and converts
     * to upper case (if alphabet).
     */
    fun toCanonical(c: Char): Char {
        var converted = toHiragana(c)
        if (converted == 'っ') {
            converted = 'つ'
        } else if (converted == 'ゃ') {
            converted = 'や'
        } else if (converted == 'ゅ') {
            converted = 'ゆ'
        } else if (converted == 'ょ') {
            converted = 'よ'
        } else if (converted == 'タ') {
            converted = '夕'
        } else if (converted == 'ロ') {
            converted = '口'
        } else if (converted == '|') {
            converted = '一'
        } else if (converted == '｜') {
            converted = '一'
        } else if (converted == 'ー') {
            converted = '一'
        }
        return converted
    }

    /**
     * Removes kana characters from the end of str.
     * Does nothing if all characters are kana.
     */
    fun removeTrailingKana(str: String): String {
        var str = str
        var newLength = str.length
        for (i in str.length - 1 downTo 0) {
            if (isKana(str[i])) {
                newLength--
            } else {
                break
            }
        }
        if (newLength > 0) {
            str = str.substring(0, newLength)
        }
        return str
    }

    /**
     * Returns true if str has trailing kana.
     */
    fun hasTrailingKana(str: String): Boolean {
        var trailKanaCount = 0
        for (i in str.length - 1 downTo 0) {
            if (isKana(str[i])) {
                trailKanaCount++
            } else {
                break
            }
        }
        return if (trailKanaCount == 0 || trailKanaCount == str.length) {
            false
        } else {
            true
        }
    }
}
