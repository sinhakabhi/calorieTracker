package com.example.calorietracker.ui.screens

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.data.Meal
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.CalorieRing
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MacroColors
import com.example.calorietracker.ui.components.MainTab
import com.example.calorietracker.ui.components.MainTabsBar
import com.example.calorietracker.ui.components.RowDivider
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.components.StatBlock
import com.example.calorietracker.ui.components.icon
import com.example.calorietracker.ui.formatDay
import com.example.calorietracker.ui.formatKcal
import com.example.calorietracker.ui.formatLitres
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.ui.theme.Accents
import com.example.calorietracker.ui.theme.tabular
import com.example.calorietracker.viewmodel.DashboardUiState
import com.example.calorietracker.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val GLASS_ML = 250
private const val BOTTLE_ML = 500
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMeal: (LocalDate) -> Unit,
    onEditMeal: (Meal) -> Unit,
    onSelectTab: (MainTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(meal: Meal) {
        viewModel.deleteMeal(meal)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${meal.name}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreMeal(meal)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddMeal(state.date) },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Log meal", style = MaterialTheme.typography.titleSmall) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            )
        },
        bottomBar = { MainTabsBar(MainTab.NUTRITION, onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            // Bottom padding keeps the last meal clear of the button.
            contentPadding = PaddingValues(start = Spacing.page, end = Spacing.page, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            item {
                ScreenTitle(
                    title = "Nutrition",
                    subtitle = formatDay(state.date) + " · " + state.date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                ) {
                    DaySwitcher(canGoForward = !state.isToday, onPrevious = viewModel::previousDay, onNext = viewModel::nextDay)
                }
            }
            item { SummaryCard(state) }
            item { WaterCard(waterMl = state.waterMl, targetMl = state.waterTargetMl, onAdd = viewModel::addWater) }

            item {
                SectionHeader("Meals") {
                    if (state.meals.isNotEmpty()) {
                        Text(
                            "${formatKcal(state.eatenKcal)} kcal",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                if (state.meals.isEmpty()) {
                    EmptyMeals()
                } else {
                    AppCard(contentPadding = PaddingValues(0.dp)) {
                        state.meals.forEachIndexed { index, meal ->
                            if (index > 0) RowDivider(Modifier.padding(start = 70.dp))
                            MealRow(meal = meal, onClick = { onEditMeal(meal) }, onDelete = { deleteWithUndo(meal) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySwitcher(canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row {
        OutlinedIconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous day")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton(onClick = onNext, enabled = canGoForward, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next day")
        }
    }
}

/** The calorie ring with eaten/target and the three macros underneath. */
@Composable
private fun SummaryCard(state: DashboardUiState) {
    val target = state.targetKcal ?: 0
    val macros = state.macroTargets
    AppCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CalorieRing(
                eatenKcal = state.eatenKcal,
                targetKcal = target,
                proteinG = state.proteinG,
                carbsG = state.carbsG,
                fatG = state.fatG,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            KcalFigure("Eaten", formatKcal(state.eatenKcal))
            KcalFigure("Target", formatKcal(target))
        }
        Spacer(Modifier.height(18.dp))
        RowDivider()
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroStat("Protein", state.proteinG, macros?.proteinG, MacroColors.Protein, Modifier.weight(1f))
            MacroStat("Carbs", state.carbsG, macros?.carbsG, MacroColors.Carbs, Modifier.weight(1f))
            MacroStat("Fat", state.fatG, macros?.fatG, MacroColors.Fat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun KcalFigure(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.tabular())
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MacroStat(label: String, eatenG: Double, targetG: Int?, color: Color, modifier: Modifier) {
    val eaten = eatenG.roundToInt()
    StatBlock(
        label = label,
        value = "$eaten",
        valueSuffix = targetG?.let { "/ $it g" } ?: "g",
        detail = targetG?.let { t -> if (t - eaten >= 0) "${t - eaten} g left" else "${eaten - t} g over" },
        color = color,
        modifier = modifier,
    )
}

/** Water drunk vs. the target, with quick-add buttons. */
@Composable
private fun WaterCard(waterMl: Int, targetMl: Int?, onAdd: (Int) -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Outlined.WaterDrop, Accents.Water)
            Spacer(Modifier.width(14.dp))
            StatBlock(
                label = "Water",
                value = formatLitres(waterMl),
                valueSuffix = if (targetMl != null) "/ ${formatLitres(targetMl)} L" else "L",
                detail = targetMl?.let { t -> if (t > waterMl) "${formatLitres(t - waterMl)} L left" else "Goal reached" },
                modifier = Modifier.weight(1f),
            )
            // Remove a glass, for mistakes.
            OutlinedIconButton(onClick = { onAdd(-GLASS_ML) }, enabled = waterMl > 0, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Remove, contentDescription = "Remove $GLASS_ML ml")
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WaterButton("+$GLASS_ML ml", Modifier.weight(1f)) { onAdd(GLASS_ML) }
            WaterButton("+$BOTTLE_ML ml", Modifier.weight(1f)) { onAdd(BOTTLE_ML) }
        }
    }
}

@Composable
private fun WaterButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Accents.Water.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.height(44.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall.tabular())
    }
}

@Composable
private fun EmptyMeals() {
    AppCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(Icons.Outlined.Restaurant, MaterialTheme.colorScheme.primary, size = 48.dp)
            Spacer(Modifier.height(12.dp))
            Text("No meals logged", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Log what you eat to track calories and macros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MealRow(meal: Meal, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(meal.mealType.icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val time = Instant.ofEpochMilli(meal.createdAtMillis).atZone(ZoneId.systemDefault()).format(timeFormat)
                Text(
                    "${meal.mealType.label} · $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "P ${formatNumber(meal.proteinG)}g · C ${formatNumber(meal.carbsG)}g · F ${formatNumber(meal.fatG)}g",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatKcal(meal.calories), style = MaterialTheme.typography.titleMedium.tabular())
                Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Delete ${meal.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
