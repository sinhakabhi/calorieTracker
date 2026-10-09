package com.example.calorietracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calorietracker.data.DayCalories
import com.example.calorietracker.ui.theme.Brand
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Bars of daily calories with a dashed line at the target. [days] is oldest first;
 * the last one is the day being viewed and is drawn at full strength.
 */
@Composable
fun WeeklyBarChart(days: List<DayCalories>, targetKcal: Int, modifier: Modifier = Modifier) {
    val overColor = MaterialTheme.colorScheme.error
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val targetLineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            if (days.isEmpty()) return@Canvas
            // Leave headroom above the tallest bar or the target line.
            val maxValue = maxOf(days.maxOf { it.calories }, targetKcal) * 1.15f
            val slot = size.width / days.size
            val barWidth = slot * 0.6f

            days.forEachIndexed { index, day ->
                val left = index * slot + (slot - barWidth) / 2
                if (day.calories == 0) {
                    // A thin stub so empty days are still visible.
                    drawRect(emptyColor, Offset(left, size.height - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()))
                } else {
                    val barHeight = size.height * (day.calories / maxValue)
                    val top = size.height - barHeight
                    val brush = if (day.calories > targetKcal) {
                        SolidColor(overColor)
                    } else {
                        Brush.verticalGradient(listOf(Brand.Pink, Brand.Violet), startY = top, endY = size.height)
                    }
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        alpha = if (index == days.lastIndex) 1f else 0.4f,
                    )
                }
            }

            val targetY = size.height * (1 - targetKcal / maxValue)
            drawLine(
                color = targetLineColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            days.forEachIndexed { index, day ->
                val weekday = LocalDate.ofEpochDay(day.epochDay).dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == days.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == days.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
