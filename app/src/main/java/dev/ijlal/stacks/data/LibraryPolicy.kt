package dev.ijlal.stacks.data

import com.google.firebase.Timestamp
import dev.ijlal.stacks.data.model.Loan
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object LibraryPolicy {
    const val LOAN_PERIOD_DAYS = 14L
    const val MAX_ACTIVE_LOANS = 3
    const val LATE_FEE_PER_DAY = 2_000L // Rupiah
}

sealed interface DueState {
    data class OnTime(val daysLeft: Long) : DueState
    data class Overdue(val daysLate: Long, val fee: Long) : DueState
    data class Returned(val returnedOn: LocalDate, val daysLate: Long, val fee: Long) : DueState
}

fun Timestamp.toLocalDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

fun Loan.dueState(today: LocalDate = LocalDate.now()): DueState {
    val due = dueAt?.toLocalDate() ?: today
    val returned = returnedAt?.toLocalDate()
    if (!isActive && returned != null) {
        val late = ChronoUnit.DAYS.between(due, returned).coerceAtLeast(0)
        return DueState.Returned(returned, late, late * LibraryPolicy.LATE_FEE_PER_DAY)
    }
    val daysLeft = ChronoUnit.DAYS.between(today, due)
    return if (daysLeft >= 0) {
        DueState.OnTime(daysLeft)
    } else {
        DueState.Overdue(-daysLeft, -daysLeft * LibraryPolicy.LATE_FEE_PER_DAY)
    }
}

fun Loan.isOverdue(today: LocalDate = LocalDate.now()): Boolean = dueState(today) is DueState.Overdue
