package com.example.calorietracker.ui

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "12" for whole numbers, "12.5" otherwise. Uses '.' so the text can be parsed back. */
fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)

/** Calories with a thousands separator for display, e.g. "2,136". */
fun formatKcal(kcal: Int): String = NumberFormat.getIntegerInstance().format(kcal)

/** Millilitres as litres without trailing zeros, e.g. 1250 → "1.25", 3500 → "3.5". */
fun formatLitres(ml: Int): String =
    BigDecimal(ml).movePointLeft(3).stripTrailingZeros().toPlainString()

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

fun formatDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(dayFormatter)
}
