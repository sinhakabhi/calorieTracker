package com.example.calorietracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.domain.CalorieCalculator
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MainTab
import com.example.calorietracker.ui.components.MainTabsBar
import com.example.calorietracker.ui.components.RowDivider
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.components.StatBlock
import com.example.calorietracker.ui.components.WeeklyBarChart
import com.example.calorietracker.ui.formatKcal
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.ui.theme.tabular
import com.example.calorietracker.viewmodel.ProgressViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val weightDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@Composable
fun ProgressScreen(viewModel: ProgressViewModel, onSelectTab: (MainTab) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogWeight by remember { mutableStateOf(false) }

    Scaffold(bottomBar = { MainTabsBar(MainTab.PROGRESS, onSelectTab) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            item { ScreenTitle(title = "Progress", subtitle = "Your last 7 days") }

            item { SectionHeader("This week") }
            item {
                AppCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBlock(
                            label = "Avg. intake",
                            value = state.averageKcal?.let(::formatKcal) ?: "—",
                            valueSuffix = "kcal",
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Within target",
                            value = "${state.daysWithinTarget}",
                            valueSuffix = "/ ${state.loggedDayCount} days",
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Days logged",
                            value = "${state.loggedDayCount}",
                            valueSuffix = "/ 7",
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
            }

            item { SectionHeader("Calories") }
            item {
                AppCard {
                    val target = state.targetKcal ?: 0
                    Text(
                        "Dashed line is your ${formatKcal(target)} kcal target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    WeeklyBarChart(days = state.week, targetKcal = target)
                }
            }

            item {
                SectionHeader("Weight") {
                    TextButton(onClick = { showLogWeight = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log weight")
                    }
                }
            }
            item {
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    val latest = state.latestWeight
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Outlined.MonitorWeight, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        StatBlock(
                            label = "Current weight",
                            value = latest?.let { formatNumber(it.weightKg) } ?: "—",
                            valueSuffix = "kg",
                            modifier = Modifier.weight(1f),
                        )
                        state.weightChangeKg?.let { change -> WeightChange(change, since = state.weights.last().epochDay) }
                    }
                    state.weights.forEachIndexed { index, entry ->
                        RowDivider()
                        Row(
                            Modifier.padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                LocalDate.ofEpochDay(entry.epochDay).format(weightDateFormat),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${formatNumber(entry.weightKg)} kg", style = MaterialTheme.typography.titleSmall.tabular())
                            // Keep at least one reading: the daily target needs a weight.
                            IconButton(onClick = { viewModel.deleteWeight(entry) }, enabled = state.weights.size > 1) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete weight entry", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogWeight) {
        LogWeightDialog(
            initial = state.latestWeight?.weightKg,
            onSave = { viewModel.logWeight(it); showLogWeight = false },
            onDismiss = { showLogWeight = false },
        )
    }
}

@Composable
private fun WeightChange(changeKg: Double, since: Long) {
    val icon = when {
        changeKg < -0.05 -> Icons.AutoMirrored.Outlined.TrendingDown
        changeKg > 0.05 -> Icons.AutoMirrored.Outlined.TrendingUp
        else -> Icons.AutoMirrored.Outlined.TrendingFlat
    }
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                (if (changeKg > 0) "+" else if (changeKg < 0) "−" else "") + "${formatNumber(Math.round(abs(changeKg) * 10) / 10.0)} kg",
                style = MaterialTheme.typography.titleSmall.tabular(),
            )
        }
        Text(
            "since ${LocalDate.ofEpochDay(since).format(DateTimeFormatter.ofPattern("d MMM"))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogWeightDialog(initial: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial?.let(::formatNumber).orEmpty()) }
    val value = text.trim().toDoubleOrNull()
    val valid = CalorieCalculator.isValidWeight(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log today's weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(6) },
                label = { Text("Weight") },
                suffix = { Text("kg") },
                isError = text.isNotBlank() && !valid,
                supportingText = { Text("Your targets update from the latest weight.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
            )
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(value!!) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Large page title used at the top of each main tab. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        trailing()
    }
}
