package dev.ijlal.stacks.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ijlal.stacks.R
import dev.ijlal.stacks.ui.theme.Midnight
import dev.ijlal.stacks.ui.theme.StacksFonts
import dev.ijlal.stacks.ui.theme.StacksType

// texture

@Composable
fun rememberGrainBrush(): ShaderBrush {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(R.drawable.grain)
    return remember(bitmap) { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
}

// film grain drawn on top of the content, alpha keeps it subtle
fun Modifier.grain(brush: Brush, alpha: Float = 0.06f): Modifier =
    drawWithContent {
        drawContent()
        drawRect(brush, alpha = alpha)
    }

// the card style used everywhere: slightly lighter background + 1dp border
fun Modifier.panel(shape: Shape = RoundedCornerShape(18.dp), color: Color = Midnight.Surface1): Modifier =
    clip(shape).background(color).border(1.dp, Midnight.Hairline, shape)

// type

// blackletter wordmark. glitch = two offset blue copies behind it
@Composable
fun Wordmark(fontSize: TextUnit, modifier: Modifier = Modifier, glitch: Boolean = false) {
    val style = TextStyle(fontFamily = StacksFonts.Blackletter, fontSize = fontSize, lineHeight = fontSize * 1.15f)
    Box(modifier) {
        if (glitch) {
            val shift = (fontSize.value / 36f).dp
            Text("Stacks", style = style, color = Midnight.IceDeep, modifier = Modifier.offset(x = -shift, y = shift / 2))
            Text("Stacks", style = style, color = Midnight.Ice.copy(alpha = 0.5f), modifier = Modifier.offset(x = shift))
        }
        Text("Stacks", style = style, color = Midnight.Cream)
    }
}

// small mono caps label
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Midnight.CreamMuted) {
    Text(text.uppercase(), style = StacksType.Eyebrow, color = color, modifier = modifier)
}

@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Midnight.Hairline))
        Icon(
            painterResource(R.drawable.ic_ornament),
            contentDescription = null,
            tint = Midnight.CreamFaint,
            modifier = Modifier.padding(horizontal = 14.dp).size(13.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(Midnight.Hairline))
    }
}

// header row on the home tabs: wordmark + user monogram
@Composable
fun TopMark(name: String?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 20.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(30.sp)
        Spacer(Modifier.weight(1f))
        if (name != null) Monogram(name, 38.dp)
    }
}

@Composable
fun PageTitle(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 24.dp)) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.displaySmall, color = Midnight.Cream)
    }
}

// status

enum class TagTone { Ice, Cream, Muted, Alert }

enum class TagGlyph { Dot, Ring, Diamond, Bang }

// Alert is solid cream instead of red, the palette has no warm colours on purpose.
@Composable
fun StatusTag(text: String, tone: TagTone, modifier: Modifier = Modifier, glyph: TagGlyph? = null) {
    val shape = RoundedCornerShape(5.dp)
    val (fill, ink, edge) = when (tone) {
        TagTone.Ice -> Triple(Midnight.IceContainer, Midnight.Ice, Midnight.IceDeep)
        TagTone.Cream -> Triple(Color.Transparent, Midnight.Cream, Midnight.CreamMuted)
        TagTone.Muted -> Triple(Color.Transparent, Midnight.CreamFaint, Midnight.Hairline)
        TagTone.Alert -> Triple(Midnight.Cream, Midnight.Void, Midnight.Cream)
    }
    Row(
        modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        when (glyph) {
            TagGlyph.Dot -> Box(Modifier.size(6.dp).clip(CircleShape).background(ink))
            TagGlyph.Ring -> Box(Modifier.size(7.dp).border(1.dp, ink, CircleShape))
            TagGlyph.Diamond -> Box(Modifier.size(6.dp).rotate(45f).background(ink))
            TagGlyph.Bang -> Text("!", style = StacksType.Stamp, color = ink)
            null -> Unit
        }
        Text(text.uppercase(), style = StacksType.Stamp, color = ink)
    }
}

// copies as little book spines: filled = on the shelf, outline = on loan
@Composable
fun ShelfMeter(available: Int, total: Int, modifier: Modifier = Modifier, spineHeight: Dp = 26.dp) {
    val heights = listOf(1f, 0.84f, 0.94f, 0.78f, 0.9f, 0.86f)
    val shape = RoundedCornerShape(1.5.dp)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        repeat(total.coerceIn(0, 24)) { index ->
            val spine = Modifier
                .width(7.dp)
                .height(spineHeight * heights[index % heights.size])
                .clip(shape)
            Box(
                if (index < available) {
                    spine.background(Midnight.Ice)
                } else {
                    spine.border(1.dp, Midnight.HairlineStrong, shape)
                },
            )
        }
    }
}

// LABEL ...... value row, like on an old library card
@Composable
fun LeaderRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Eyebrow(label, color = Midnight.CreamFaint)
        Box(
            Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 10.dp, bottom = 4.dp)
                .height(1.dp)
                .drawBehind {
                    drawLine(
                        color = Midnight.HairlineStrong,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 4.dp.toPx())),
                    )
                },
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Midnight.Cream)
    }
}

// user's initial in blackletter
@Composable
fun Monogram(name: String, size: Dp, modifier: Modifier = Modifier) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Midnight.IceContainer, Midnight.Surface1)))
            .border(1.dp, Midnight.IceDeep, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            fontFamily = StacksFonts.Blackletter,
            fontSize = (size.value * 0.5f).sp,
            lineHeight = (size.value * 0.5f).sp,
            color = Midnight.Cream,
            modifier = Modifier.offset(y = -(size / 40)),
        )
    }
}

// controls

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    arrow: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Midnight.Cream,
            contentColor = Midnight.Void,
            disabledContainerColor = Midnight.Surface3,
            disabledContentColor = Midnight.CreamFaint,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.height(56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
            if (arrow) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = Midnight.Cream,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Midnight.HairlineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = Midnight.CreamFaint),
        modifier = modifier.height(46.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun FilterTag(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(Midnight.Cream)
                } else {
                    Modifier.border(1.dp, Midnight.HairlineStrong, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Text(text.uppercase(), style = StacksType.Stamp, color = if (selected) Midnight.Void else Midnight.CreamMuted)
    }
}

// text field with only an underline, the line turns blue when focused
@Composable
fun MidnightField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onDone: () -> Unit = {},
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val lineColor = when {
        error != null -> Midnight.Frost
        focused -> Midnight.Ice
        else -> Midnight.HairlineStrong
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = Midnight.Cream, fontSize = 18.sp, lineHeight = 26.sp)

    Column(modifier) {
        Eyebrow(label, color = if (focused) Midnight.Ice else Midnight.CreamFaint)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(Midnight.Ice),
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, capitalization = capitalization),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            decorationBox = { inner ->
                Row(Modifier.padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { inner() }
                    if (password) {
                        Text(
                            if (revealed) "HIDE" else "SHOW",
                            style = StacksType.Stamp,
                            color = Midnight.CreamMuted,
                            modifier = Modifier
                                .clickable { revealed = !revealed }
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (focused) 1.5.dp else 1.dp)
                .background(lineColor),
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = Midnight.Frost,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun SearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .panel(RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Midnight.CreamFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Midnight.Cream),
            cursorBrush = SolidColor(Midnight.Ice),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .testTag("search"),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder.uppercase(), style = StacksType.Eyebrow, color = Midnight.CreamFaint)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = Midnight.CreamMuted)
            }
        }
    }
}
