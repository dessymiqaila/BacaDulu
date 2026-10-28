package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostWithSite

@Composable
fun PostCardItem(
    post: PostWithSite,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.background
    val isDark = (0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) < 0.5

    val cardBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val cardBorder = if (isDark) Color(0xFF262626) else Color(0xFFE5E7EB)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar on the left
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (post.isBookmarked == 1) {
                                listOf(Color(0xFFEAB308), Color(0xFFF59E0B))
                            } else {
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            }
                        )
                    )
            )

            // Post Title ONLY
            Text(
                text = post.postTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 0.15.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )

            // Bookmark icon button
            IconButton(
                onClick = onBookmarkToggle,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .testTag("bookmark_button_${post.id}")
            ) {
                Icon(
                    imageVector = if (post.isBookmarked == 1) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (post.isBookmarked == 1) "Hapus Bookmark" else "Simpan Bookmark",
                    tint = if (post.isBookmarked == 1) Color(0xFFEAB308) else (if (isDark) Color(0xFF888888) else Color(0xFF777777))
                )
            }
        }
    }
}
