package com.example.calorietracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorietracker.ui.formatKcal
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Calories eaten vs. the daily target. The filled part is one continuous arc whose colour
 * changes by macro (protein, carbs, fat), sized by the calories each one supplied
 * (4 / 4 / 9 kcal per gram). Tapping the ring swaps the centre text for the macro split.
 * Drawn in light tones for the dark gradient hero card.
 */
@Composable
fun CalorieRing(
    eatenKcal: Int,
    targetKcal: Int,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    modifier: Modifier = Modifier,
) {
    var showSplit by rememberSaveable { mutableStateOf(false) }

    val isOver = eatenKcal > targetKcal
    val fill = if (targetKcal > 0) (eatenKcal.toFloat() / targetKcal).coerceIn(0f, 1f) else 0f

    val proteinKcal = proteinG * 4
    val carbsKcal = carbsG * 4
    val fatKcal = fatG * 9
    val macroKcal = proteinKcal + carbsKcal + fatKcal

    // Each segment's share of the full circle (0..1). Without macro data, the fill is one neutral arc.
    fun share(kcal: Double) = if (macroKcal > 0) (fill * kcal / macroKcal).toFloat() else 0f
    val animation = tween<Float>(durationMillis = 800)
    val protein by animateFloatAsState(share(proteinKcal), animation, label = "protein")
    val carbs by animateFloatAsState(share(carbsKcal), animation, label = "carbs")
    val fat by animateFloatAsState(share(fatKcal), animation, label = "fat")
    val unknown by animateFloatAsState(if (macroKcal > 0) 0f else fill, animation, label = "unknown")

    Box(
        modifier = modifier
            .size(220.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Show macro split",
            ) { showSplit = !showSplit },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                Color.White.copy(alpha = 0.14f), 0f, 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth),
            )

            val segments = listOf(
                MacroColors.Protein to protein,
                MacroColors.Carbs to carbs,
                MacroColors.Fat to fat,
                Color.White.copy(alpha = 0.85f) to unknown,
            ).filter { it.second > 0.0005f }
            if (segments.isEmpty()) return@Canvas

            // Flat-ended segments placed end to end make one continuous arc; each overlaps the
            // next by a hair so no seam shows. Only the arc's two outer ends get rounded caps.
            val butt = Stroke(strokeWidth, cap = StrokeCap.Butt)
            var angle = -90f
            segments.forEachIndexed { index, (color, fraction) ->
                val sweep = fraction * 360f
                val overlap = if (index < segments.lastIndex) 0.6f else 0f
                drawArc(color, angle, sweep + overlap, useCenter = false, topLeft = topLeft, size = arcSize, style = butt)
                angle += sweep
            }
            val radius = arcSize.width / 2
            fun pointAt(degrees: Float): Offset {
                val rad = Math.toRadians(degrees.toDouble())
                return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
            }
            drawCircle(segments.last().first, strokeWidth / 2, pointAt(angle))
            drawCircle(segments.first().first, strokeWidth / 2, pointAt(-90f))
        }

        AnimatedContent(
            targetState = showSplit,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "ringCentre",
        ) { split ->
            if (split) {
                MacroSplit(proteinKcal, carbsKcal, fatKcal)
            } else {
                RemainingKcal(eatenKcal, targetKcal, isOver)
            }
        }
    }
}

@Composable
private fun RemainingKcal(eatenKcal: Int, targetKcal: Int, isOver: Boolean) {
    val overColor = Color(0xFFFFB4B4)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatKcal(abs(targetKcal - eatenKcal)),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isOver) overColor else Color.White,
        )
        Text(
            text = if (isOver) "kcal over" else "kcal left",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isOver) overColor else Color.White.copy(alpha = 0.85f),
        )
        Text(
            text = "${formatKcal(eatenKcal)} / ${formatKcal(targetKcal)} eaten",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

/** Calories from each macro, colour-coded to match the ring and the macro tiles. */
@Composable
private fun MacroSplit(proteinKcal: Double, carbsKcal: Double, fatKcal: Double) {
    val total = proteinKcal + carbsKcal + fatKcal
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SplitRow(proteinKcal, total, MacroColors.Protein)
        SplitRow(carbsKcal, total, MacroColors.Carbs)
        SplitRow(fatKcal, total, MacroColors.Fat)
    }
}

@Composable
private fun SplitRow(kcal: Double, total: Double, color: Color) {
    val percent = if (total > 0) (kcal / total * 100).roundToInt() else 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            "${formatKcal(kcal.roundToInt())} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(92.dp),
        )
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
