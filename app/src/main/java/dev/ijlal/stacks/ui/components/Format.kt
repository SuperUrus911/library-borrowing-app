package dev.ijlal.stacks.ui.components

import com.google.firebase.Timestamp
import dev.ijlal.stacks.data.toLocalDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
private val ShortDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.US)

fun LocalDate.formatted(): String = format(DateFormat)

fun Timestamp?.formattedDate(): String = this?.toLocalDate()?.formatted() ?: "—"

fun Timestamp?.formattedShortDate(): String = this?.toLocalDate()?.format(ShortDateFormat) ?: "—"

fun formatRupiah(amount: Long): String = "Rp" + "%,d".format(Locale.US, amount).replace(',', '.')

fun Timestamp?.relativeTime(now: Instant = Instant.now()): String {
    if (this == null) return ""
    val elapsed = Duration.between(toInstant(), now)
    return when {
        elapsed.toMinutes() < 1 -> "just now"
        elapsed.toMinutes() < 60 -> "${elapsed.toMinutes()}m ago"
        elapsed.toHours() < 24 -> "${elapsed.toHours()}h ago"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}d ago"
        else -> formattedShortDate()
    }
}

fun pluralize(count: Number, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count.toLong() == 1L) singular else plural}"
