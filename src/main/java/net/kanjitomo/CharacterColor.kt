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

package net.kanjitomo

/**
 * Character and background color.
 */
enum class CharacterColor {
    /**
     * Character color is detected automatically. (default)
     */
    AUTOMATIC,

    /**
     * Black characters over white background
     */
    BLACK_ON_WHITE,

    /**
     * White characters over black background
     */
    WHITE_ON_BLACK
}
