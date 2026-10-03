package com.example.time

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object TimeCalculator {

    data class ElapsedResult(
        val isNegative: Boolean,
        val days: Long,
        val hours: Long,
        val minutes: Long,
        val formatted: String
    )

    data class ShiftResult(
        val grossMinutes: Int,
        val grossHours: Int,
        val grossRemainMin: Int,
        val breakMinutes: Int,
        val netMinutes: Int,
        val netHours: Int,
        val netRemainMin: Int,
        val netDecimalHours: Double,
        val crossesMidnight: Boolean,
        val formattedSummary: String
    )

    fun calculateElapsed(startMillis: Long, endMillis: Long): ElapsedResult {
        var diff = endMillis - startMillis
        val isNegative = diff < 0
        diff = abs(diff)

        val days = diff / (24 * 60 * 60 * 1000)
        val hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)

        val sign = if (isNegative) "−" else ""
        val formatted = "$sign${days}d ${hours}h ${minutes}m"
        return ElapsedResult(isNegative, days, hours, minutes, formatted)
    }

    fun calculateShift(inHour: Int, inMinute: Int, outHour: Int, outMinute: Int, breakMinutes: Int): ShiftResult {
        var diffMinutes = (outHour * 60 + outMinute) - (inHour * 60 + inMinute)
        val crossesMidnight = diffMinutes < 0
        if (crossesMidnight) {
            diffMinutes += 24 * 60
        }

        val grossMinutes = diffMinutes
        val grossHours = grossMinutes / 60
        val grossRemainMin = grossMinutes % 60

        val netMinutes = (grossMinutes - breakMinutes).coerceAtLeast(0)
        val netHours = netMinutes / 60
        val netRemainMin = netMinutes % 60
        val netDecimal = netMinutes / 60.0

        val breakNote = if (breakMinutes > 0) " minus ${breakMinutes}m break" else ""
        val formattedSummary = if (breakMinutes > 0) {
            "${netHours}h ${netRemainMin}m net  ($netMinutes min • ${String.format(Locale.US, "%.2f", netDecimal)} hrs)"
        } else {
            "${grossHours}h ${grossRemainMin}m  ($grossMinutes min • ${String.format(Locale.US, "%.2f", netDecimal)} hrs)"
        }

        return ShiftResult(
            grossMinutes = grossMinutes,
            grossHours = grossHours,
            grossRemainMin = grossRemainMin,
            breakMinutes = breakMinutes,
            netMinutes = netMinutes,
            netHours = netHours,
            netRemainMin = netRemainMin,
            netDecimalHours = netDecimal,
            crossesMidnight = crossesMidnight,
            formattedSummary = formattedSummary
        )
    }

    fun calculateAddSub(baseMillis: Long, amount: Double, unit: String, isAdd: Boolean): String {
        val msPerUnit = when (unit.lowercase()) {
            "minutes" -> 60_000L
            "hours" -> 3_600_000L
            "days" -> 86_400_000L
            else -> 3_600_000L
        }
        val delta = (amount * msPerUnit).toLong() * (if (isAdd) 1 else -1)
        val resultMillis = baseMillis + delta

        val sdf = SimpleDateFormat("EEE, MMM d, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(resultMillis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateOnly(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatTimeOnly(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }
}
