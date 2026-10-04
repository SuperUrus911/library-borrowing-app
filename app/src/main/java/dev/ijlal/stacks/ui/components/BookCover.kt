package dev.ijlal.stacks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksFonts
import dev.ijlal.stacks.ui.theme.StacksType
import kotlin.math.absoluteValue

// colours for the generated covers
private val Bindings = listOf(
    Color(0xFF1B2433) to Color(0xFF34465F),
    Color(0xFF13201F) to Color(0xFF2B4441),
    Color(0xFF211B2B) to Color(0xFF433653),
    Color(0xFF1A1D22) to Color(0xFF3A3F47),
    Color(0xFF0F1726) to Color(0xFF243A5A),
    Color(0xFF1C2027) to Color(0xFF48505E),
)

// Shows the Open Library cover if there is one. The generated cover is drawn underneath,
// so if the image is missing or fails to load you just see that instead.
@Composable
fun BookCover(
    title: String,
    author: String,
    coverUrl: String?,
    width: Dp,
    modifier: Modifier = Modifier,
    elevation: Dp = 6.dp,
) {
    val height = width * 1.5f
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 5.dp, bottomEnd = 5.dp)
    val (dark, light) = Bindings[title.hashCode().absoluteValue % Bindings.size]
    val large = width >= 96.dp
    val inset = width * 0.07f

    Box(
        modifier
            .width(width)
            .height(height)
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.linearGradient(listOf(light, dark))),
    ) {
        // thin inset frame
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = inset * 1.6f, end = inset, top = inset, bottom = inset)
                .border(0.75.dp, Midnight.Cream.copy(alpha = 0.22f), RoundedCornerShape(1.dp)),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = width * 0.17f, end = width * 0.11f, top = height * 0.11f, bottom = height * 0.08f),
        ) {
            Text(
                text = title,
                color = Midnight.Cream,
                fontFamily = StacksFonts.Serif,
                fontSize = if (large) 19.sp else (width.value / 5.6f).sp,
                lineHeight = if (large) 21.sp else (width.value / 5f).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            if (large) {
                Text(
                    text = author.uppercase(),
                    color = Midnight.Cream.copy(alpha = 0.7f),
                    style = StacksType.Stamp.copy(fontSize = 8.sp, lineHeight = 10.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // darker strip for the spine
        Box(
            Modifier
                .fillMaxHeight()
                .width(width * 0.06f)
                .background(Color.Black.copy(alpha = 0.28f))
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
