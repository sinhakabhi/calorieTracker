package com.veyra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyra.app.ui.components.AppCard
import com.veyra.app.ui.components.IconBadge
import com.veyra.app.ui.components.MacroColors
import com.veyra.app.ui.components.MainTab
import com.veyra.app.ui.components.MainTabsBar
import com.veyra.app.ui.components.SectionHeader
import com.veyra.app.ui.components.Spacing
import com.veyra.app.ui.components.StatBlock
import com.veyra.app.ui.formatKcal
import com.veyra.app.ui.formatLitres
import com.veyra.app.ui.formatNumber
import com.veyra.app.ui.theme.Accents
import com.veyra.app.ui.theme.overline
import com.veyra.app.ui.theme.tabular
import com.veyra.app.viewmodel.TodayUiState
import com.veyra.app.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

private fun greeting(time: LocalTime = LocalTime.now()) = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** The daily hub: a summary of food, training, water and weight, linking into each tab. */
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onSelectTab: (MainTab) -> Unit,
    onLogFood: () -> Unit,
    onCreatePlan: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(bottomBar = { MainTabsBar(MainTab.TODAY, onSelectTab) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),
                            style = MaterialTheme.typography.labelSmall.overline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        val name = state.account?.firstName.orEmpty()
                        Text(
                            if (name.isBlank()) "${greeting()}." else "${greeting()},\n$name.",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Eat well, move well, one day at a time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedIconButton(onClick = onOpenNotifications, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                }
            }

            item { SectionHeader("Today's fuel") }
            item { FuelCard(state, onOpen = { onSelectTab(MainTab.NUTRITION) }, onLogFood = onLogFood) }

            item { SectionHeader("Your next move") }
            item { NextMoveCard(state, onOpen = { onSelectTab(MainTab.TRAINING) }, onCreatePlan = onCreatePlan) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.item)) {
                    WaterTile(state, onAdd = { viewModel.addWater(250) }, modifier = Modifier.weight(1f))
                    WeightTile(state, onOpen = { onSelectTab(MainTab.PROGRESS) }, modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Targets come from your profile. Adjust them anytime in Profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelCard(state: TodayUiState, onOpen: () -> Unit, onLogFood: () -> Unit) {
    val target = state.targets?.kcal ?: 0
    val macros = state.targets?.macros
    val remaining = target - state.eatenKcal
    val over = remaining < 0
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatKcal(state.eatenKcal), style = MaterialTheme.typography.displaySmall.tabular())
            Spacer(Modifier.width(6.dp))
            Text(
                "/ ${formatKcal(target)} kcal",
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp).weight(1f),
            )
            // "1,527 left" / "120 over" pill.
            val pillColor = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .background(pillColor.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "${formatKcal(abs(remaining))} ${if (over) "over" else "left"}",
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = pillColor,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (target > 0) (state.eatenKcal.toFloat() / target).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroFigure("Protein", state.proteinG, macros?.proteinG, MacroColors.Protein, Modifier.weight(1f))
            MacroFigure("Carbs", state.carbsG, macros?.carbsG, MacroColors.Carbs, Modifier.weight(1f))
            MacroFigure("Fat", state.fatG, macros?.fatG, MacroColors.Fat, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val count = state.meals.size
            Text(
                when (count) {
                    0 -> "No meals logged yet"
                    1 -> "1 meal logged"
                    else -> "$count meals logged"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onLogFood) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log food")
            }
        }
    }
}

@Composable
private fun MacroFigure(label: String, eatenG: Double, targetG: Int?, color: Color, modifier: Modifier) {
    StatBlock(
        label = label,
        value = "${eatenG.roundToInt()}",
        valueSuffix = targetG?.let { "/ $it g" } ?: "g",
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun NextMoveCard(state: TodayUiState, onOpen: () -> Unit, onCreatePlan: () -> Unit) {
    val workout = state.workout
    if (!workout.hasPlan) {
        AppCard(onClick = onCreatePlan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.FitnessCenter, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Plan your training week", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Build it yourself or let AI draft it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val isRest = workout.total == 0
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (isRest) Icons.Outlined.SelfImprovement else Icons.Outlined.FitnessCenter, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isRest) "Rest day" else "Today's workout",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    workout.day?.title ?: "Training",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Open training", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        if (isRest) {
            Text(
                "Nothing scheduled. Recover well, or add a session in Training.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val complete = workout.done == workout.total
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { workout.done.toFloat() / workout.total },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = if (complete) Accents.Success else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (complete) "Done" else "${workout.done} / ${workout.total}",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                )
            }
        }
    }
}

@Composable
private fun WaterTile(state: TodayUiState, onAdd: () -> Unit, modifier: Modifier) {
    val target = state.targets?.waterMl
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.WaterDrop, Accents.Water, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Water", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        StatBlock(
            label = "",
            value = formatLitres(state.waterMl),
            valueSuffix = target?.let { "/ ${formatLitres(it)} L" } ?: "L",
        )
        Spacer(Modifier.height(10.dp))
        FilledTonalButton(
            onClick = onAdd,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Accents.Water.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text("+250 ml", style = MaterialTheme.typography.labelLarge.tabular())
        }
    }
}

@Composable
private fun WeightTile(state: TodayUiState, onOpen: () -> Unit, modifier: Modifier) {
    AppCard(modifier, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.MonitorWeight, MaterialTheme.colorScheme.primary, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        StatBlock(label = "", value = state.latestWeight?.let { formatNumber(it.weightKg) } ?: "—", valueSuffix = "kg")
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                state.latestWeight?.let {
                    "Logged " + LocalDate.ofEpochDay(it.epochDay).format(DateTimeFormatter.ofPattern("d MMM"))
                } ?: "Not logged",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
