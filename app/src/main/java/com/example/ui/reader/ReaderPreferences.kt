package com.example.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ReaderFont(val label: String, val fontFamily: FontFamily) {
    ROBOTO("Roboto / Sans", FontFamily.SansSerif),
    SERIF("Merriweather / Serif", FontFamily.Serif),
    MONO("JetBrains / Mono", FontFamily.Monospace),
    CURSIVE("Kaligrafi / Cursive", FontFamily.Cursive),
    DEFAULT("Sistem Default", FontFamily.Default)
}

enum class ReaderThemePreset(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    LIGHT(
        label = "Terang",
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF000000)
    ),
    DARK(
        label = "Gelap",
        backgroundColor = Color(0xFF000000),
        textColor = Color(0xFFFFFFFF)
    ),
    SEPIA(
        label = "Sepia Kertas",
        backgroundColor = Color(0xFFFBF0D9),
        textColor = Color(0xFF3C2415)
    )
}

val RecommendedTextColors = listOf(
    Pair("Hitam Pekat", Color(0xFF000000)),
    Pair("Charcoal", Color(0xFF334155)),
    Pair("Cokelat", Color(0xFF451A03)),
    Pair("Biru Tua", Color(0xFF1E3A8A)),
    Pair("Hijau Tua", Color(0xFF064E3B)),
    Pair("Abu Terang", Color(0xFFCBD5E1)),
    Pair("Krem", Color(0xFFFEF3C7)),
    Pair("Putih Bersih", Color(0xFFFFFFFF))
)

data class ReaderSettings(
    val font: ReaderFont = ReaderFont.ROBOTO,
    val fontSizeSp: Float = 18f,
    val themePreset: ReaderThemePreset = ReaderThemePreset.LIGHT,
    val customTextColor: Color? = null,
    val customBackgroundColor: Color? = null,
    // Spacing controls:
    val paragraphSpacingDp: Float = 14f,     // Jarak antar paragraf <p>
    val lineBreakSpacingDp: Float = 8f,      // Jarak baris baru <br/>
    val lineHeightSp: Float = 26f            // Tinggi baris (line height) saat baris baru/wrapping non-HTML
) {
    val activeBackgroundColor: Color
        get() = customBackgroundColor ?: themePreset.backgroundColor

    val activeTextColor: Color
        get() = customTextColor ?: themePreset.textColor

    companion object {
        val DEFAULT = ReaderSettings()
    }
}
