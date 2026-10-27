package com.example.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ReaderFont(val label: String, val fontFamily: FontFamily) {
    ROBOTO("Roboto / Sans", FontFamily.SansSerif),
    SERIF("Serif / Buku", FontFamily.Serif),
    MONO("Monospace", FontFamily.Monospace),
    CURSIVE("Cursive", FontFamily.Cursive),
    DEFAULT("Sistem", FontFamily.Default)
}

enum class ReaderThemePreset(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    LIGHT(
        label = "Terang",
        backgroundColor = Color(0xFFFBFBFB),
        textColor = Color(0xFF1E293B)
    ),
    DARK(
        label = "Gelap",
        backgroundColor = Color(0xFF0F172A),
        textColor = Color(0xFFE2E8F0)
    ),
    SEPIA(
        label = "Sepia Kertas",
        backgroundColor = Color(0xFFFBF0D9),
        textColor = Color(0xFF3C2415)
    )
}

val RecommendedTextColors = listOf(
    Pair("Hitam", Color(0xFF111827)),
    Pair("Charcoal", Color(0xFF334155)),
    Pair("Cokelat", Color(0xFF451A03)),
    Pair("Biru Tua", Color(0xFF1E3A8A)),
    Pair("Hijau Tua", Color(0xFF064E3B)),
    Pair("Abu Terang", Color(0xFFCBD5E1)),
    Pair("Krem", Color(0xFFFEF3C7)),
    Pair("Putih", Color(0xFFFFFFFF))
)

data class ReaderSettings(
    val font: ReaderFont = ReaderFont.ROBOTO,
    val fontSizeSp: Float = 18f,
    val themePreset: ReaderThemePreset = ReaderThemePreset.LIGHT,
    val customTextColor: Color? = null,
    val customBackgroundColor: Color? = null
) {
    val activeBackgroundColor: Color
        get() = customBackgroundColor ?: themePreset.backgroundColor

    val activeTextColor: Color
        get() = customTextColor ?: themePreset.textColor

    companion object {
        val DEFAULT = ReaderSettings()
    }
}
