package dev.ijlal.stacks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.absoluteValue

private val CoverPalettes = listOf(
    Color(0xFF2E4057) to Color(0xFF4F6D8A),
    Color(0xFF7A3B2E) to Color(0xFFA65A44),
    Color(0xFF2F5D50) to Color(0xFF4E8C77),
    Color(0xFF5B3F6B) to Color(0xFF8566A0),
    Color(0xFF8A6A1F) to Color(0xFFB89439),
    Color(0xFF3B3B58) to Color(0xFF5F5F8A),
    Color(0xFF6B2D3C) to Color(0xFF9A4A5E),
)

/**
 * Real cover art when Open Library has it, otherwise a generated cloth-bound cover.
 * The generated cover is drawn underneath, so a missing or failed image simply reveals it.
 */
@Composable
fun BookCover(
    title: String,
    author: String,
    coverUrl: String?,
    width: Dp,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
) {
    val height = width * 1.5f
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    val (dark, light) = CoverPalettes[title.hashCode().absoluteValue % CoverPalettes.size]
    val large = width >= 96.dp

    Box(
        modifier
            .width(width)
            .height(height)
            .shadow(elevation, shape)
            .clip(shape)
            .background(Brush.linearGradient(listOf(light, dark))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = width * 0.16f, end = width * 0.08f, top = height * 0.1f, bottom = height * 0.08f),
        ) {
            Text(
                text = title,
                color = Color(0xFFF6EFE3),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (large) 17.sp else (width.value / 6.5f).sp,
                lineHeight = if (large) 20.sp else (width.value / 5.5f).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            if (large) {
                Text(
                    text = author,
                    color = Color(0xFFF6EFE3).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Spine shading on the left edge.
        Box(
            Modifier
                .fillMaxHeight()
                .width(width * 0.07f)
                .background(Color.Black.copy(alpha = 0.18f))
                .align(Alignment.CenterStart),
        )
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Cover of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
