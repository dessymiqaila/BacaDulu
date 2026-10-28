package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostWithSite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostReaderScreen(
    post: PostWithSite?,
    prevPostId: Int?,
    nextPostId: Int?,
    readerSettings: ReaderSettings,
    showRawContent: Boolean,
    showSettingsDialog: Boolean,
    onBackClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onToggleRawContent: () -> Unit,
    onToggleSettings: (Boolean) -> Unit,
    onFontSelected: (ReaderFont) -> Unit,
    onFontSizeDelta: (Float) -> Unit,
    onFontSizeSet: (Float) -> Unit,
    onThemeSelected: (ReaderThemePreset) -> Unit,
    onTextColorSelected: (Color) -> Unit,
    onParagraphSpacingSet: (Float) -> Unit,
    onLineBreakSpacingSet: (Float) -> Unit,
    onLineHeightSet: (Float) -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = readerSettings.activeBackgroundColor
    val textColor = readerSettings.activeTextColor
    val fontFamily = readerSettings.font.fontFamily
    val fontSize = readerSettings.fontSizeSp.sp

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Double tap Quick HUD state (< ^ >)
    var showQuickHud by remember { mutableStateOf(false) }

    // Disappear automatically when scrolling
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            showQuickHud = false
        }
    }

    val parsedParagraphs = remember(post?.postContent, textColor) {
        if (post != null) {
            HtmlContentFormatter.parseContentToParagraphs(post.postContent, textColor)
        } else emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = post?.postTitle ?: "Membaca Pos",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        if (post != null) {
                            Text(
                                text = post.siteUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    if (post != null) {
                        IconButton(
                            onClick = { onBookmarkToggle(post.id, post.isBookmarked == 1) },
                            modifier = Modifier.testTag("reader_bookmark_toggle")
                        ) {
                            Icon(
                                imageVector = if (post.isBookmarked == 1) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (post.isBookmarked == 1) Color(0xFFEAB308) else textColor
                            )
                        }
                    }
                    IconButton(
                        onClick = { onToggleSettings(true) },
                        modifier = Modifier.testTag("reader_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Teks",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            showQuickHud = !showQuickHud
                        }
                    )
                }
        ) {
            if (post == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Memuat pos...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Header Post Info
                    Text(
                        text = post.postTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = fontFamily,
                            fontSize = (readerSettings.fontSizeSp + 6).sp,
                            lineHeight = (readerSettings.lineHeightSp + 10).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = textColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = post.siteUrl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = post.postDate.take(16),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Article Content
                    if (!showRawContent) {
                        parsedParagraphs.forEachIndexed { pIndex, p ->
                            p.segments.forEachIndexed { sIndex, segment ->
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = fontFamily,
                                        fontSize = fontSize,
                                        lineHeight = readerSettings.lineHeightSp.sp
                                    ),
                                    color = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (sIndex < p.segments.size - 1) {
                                    Spacer(modifier = Modifier.height(readerSettings.lineBreakSpacingDp.dp))
                                }
                            }
                            if (pIndex < parsedParagraphs.size - 1) {
                                Spacer(modifier = Modifier.height(readerSettings.paragraphSpacingDp.dp))
                            }
                        }
                    } else {
                        // Raw Unmodified HTML Mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reader_raw_content"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (readerSettings.themePreset == ReaderThemePreset.DARK) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Konten Mentah (Tanpa Pembersihan)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = post.postContent,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = if (readerSettings.themePreset == ReaderThemePreset.DARK) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // "Lihat Asli" button at bottom of post
                    Button(
                        onClick = onToggleRawContent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_raw_content_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showRawContent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (showRawContent) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (showRawContent) Icons.Default.Visibility else Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showRawContent) "Kembali ke Format Baca Rapi" else "Lihat Asli (Raw HTML)"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Permanent navigation buttons at bottom of content
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = textColor.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Navigasi Artikel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onPrevClick,
                                    enabled = prevPostId != null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("prev_post_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sebelumnya")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                OutlinedButton(
                                    onClick = onNextClick,
                                    enabled = nextPostId != null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("next_post_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Berikutnya")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scroll to Top inline button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scroll_to_top_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kembali ke Atas")
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            // Quick Floating HUD Overlay on Double Tap (< ^ >)
            AnimatedVisibility(
                visible = showQuickHud,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .testTag("floating_quick_nav_hud")
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (readerSettings.themePreset == ReaderThemePreset.DARK) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFF0F172A).copy(alpha = 0.92f),
                    contentColor = Color.White,
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // < Previous
                        IconButton(
                            onClick = onPrevClick,
                            enabled = prevPostId != null,
                            modifier = Modifier.testTag("hud_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Sebelumnya",
                                tint = if (prevPostId != null) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // ^ Scroll to Top
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(0)
                                    showQuickHud = false
                                }
                            },
                            modifier = Modifier.testTag("hud_totop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Kembali ke Atas",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // > Next
                        IconButton(
                            onClick = onNextClick,
                            enabled = nextPostId != null,
                            modifier = Modifier.testTag("hud_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Berikutnya",
                                tint = if (nextPostId != null) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Reader Settings Modal Bottom Sheet
    if (showSettingsDialog) {
        ModalBottomSheet(
            onDismissRequest = { onToggleSettings(false) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("reader_settings_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengaturan Teks & Tampilan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = onResetSettings,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reset_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Family Selector
                Text(
                    text = "Pilihan Font",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReaderFont.values().forEach { font ->
                        FilterChip(
                            selected = readerSettings.font == font,
                            onClick = { onFontSelected(font) },
                            label = { Text(font.label) },
                            modifier = Modifier.testTag("font_chip_${font.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size Controls
                Text(
                    text = "Ukuran Huruf (${readerSettings.fontSizeSp.toInt()} sp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedIconButton(
                        onClick = { onFontSizeDelta(-1f) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("font_decrease_button")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Perkecil Huruf")
                    }

                    Slider(
                        value = readerSettings.fontSizeSp,
                        onValueChange = onFontSizeSet,
                        valueRange = 14f..32f,
                        steps = 8,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .testTag("font_size_slider")
                    )

                    OutlinedIconButton(
                        onClick = { onFontSizeDelta(1f) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("font_increase_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Perbesar Huruf")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Paragraph Spacing (<p>)
                Text(
                    text = "Jarak Antar Paragraf <p> (${readerSettings.paragraphSpacingDp.toInt()} dp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = readerSettings.paragraphSpacingDp,
                    onValueChange = onParagraphSpacingSet,
                    valueRange = 4f..36f,
                    steps = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paragraph_spacing_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Line Break Spacing (<br/>)
                Text(
                    text = "Jarak Baris Baru <br/> (${readerSettings.lineBreakSpacingDp.toInt()} dp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = readerSettings.lineBreakSpacingDp,
                    onValueChange = onLineBreakSpacingSet,
                    valueRange = 2f..24f,
                    steps = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("linebreak_spacing_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Line Height (non-HTML newlines / wrapping lines)
                Text(
                    text = "Tinggi Baris / Line Height (${readerSettings.lineHeightSp.toInt()} sp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = readerSettings.lineHeightSp,
                    onValueChange = onLineHeightSet,
                    valueRange = 18f..42f,
                    steps = 11,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("line_height_slider")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Text Color Palette (Warna Huruf)
                Text(
                    text = "Warna Huruf",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RecommendedTextColors.forEach { (name, color) ->
                        val isSelected = readerSettings.activeTextColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { onTextColorSelected(color) }
                                .testTag("text_color_$name"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (color == Color.White) Color.Black else Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Theme Presets (Light, Dark, Sepia)
                Text(
                    text = "Tema & Warna Latar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderThemePreset.values().forEach { preset ->
                        FilterChip(
                            selected = readerSettings.themePreset == preset && readerSettings.customBackgroundColor == null,
                            onClick = { onThemeSelected(preset) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(preset.backgroundColor, CircleShape)
                                            .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(preset.label)
                                }
                            },
                            modifier = Modifier.testTag("theme_chip_${preset.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
