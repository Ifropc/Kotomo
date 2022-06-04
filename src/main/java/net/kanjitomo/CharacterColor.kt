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
