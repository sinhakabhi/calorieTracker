package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.data.Meal
import com.example.calorietracker.ui.components.CalorieRing
import com.example.calorietracker.ui.components.MacroColors
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.StatTile
import com.example.calorietracker.ui.components.WeeklyBarChart
import com.example.calorietracker.ui.components.color
import com.example.calorietracker.ui.formatDay
import com.example.calorietracker.ui.formatKcal
import com.example.calorietracker.ui.formatLitres
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.ui.theme.Brand
import com.example.calorietracker.viewmodel.DashboardUiState
import com.example.calorietracker.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMeal: (LocalDate) -> Unit,
    onEditMeal: (Meal) -> Unit,
    onEditProfile: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val target = state.targetKcal ?: 0

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
        floatingActionButton = { AddMealButton(onClick = { onAddMeal(state.date) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            // Bottom padding keeps the last meal clear of the FAB.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(onEditProfile = onEditProfile) }
            item {
                DateSwitcher(
                    label = formatDay(state.date),
                    canGoForward = !state.isToday,
                    onPrevious = viewModel::previousDay,
                    onNext = viewModel::nextDay,
                )
            }
            item { HeroCard(state = state, targetKcal = target) }
            item { MacroTiles(state) }
            item {
                WaterCard(
                    waterMl = state.waterMl,
                    targetMl = state.waterTargetMl,
                    onAdd = viewModel::addWater,
                )
            }
            item {
                SectionCard(
                    title = "Last 7 days",
                    subtitle = "Dashed line is your ${formatKcal(target)} kcal target",
                ) {
                    WeeklyBarChart(days = state.week, targetKcal = target)
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Meals",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.meals.isNotEmpty()) {
                        Text(
                            "${state.meals.size} logged · ${formatKcal(state.eatenKcal)} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.meals.isEmpty()) {
                item { EmptyMeals() }
            }
            items(state.meals, key = { it.id }) { meal ->
                MealRow(meal = meal, onClick = { onEditMeal(meal) }, onDelete = { deleteWithUndo(meal) })
            }
        }
    }
}

private fun greeting(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun Header(onEditProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${greeting()} 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Brand.HeroGradient),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onEditProfile) {
                Icon(Icons.Filled.Person, contentDescription = "Edit profile", tint = Color.White)
            }
        }
    }
}

@Composable
private fun DateSwitcher(label: String, canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                Text(
                    label,
                    modifier = Modifier.width(140.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onNext, enabled = canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
        }
    }
}

/** The gradient card with the calorie ring, split by macro. */
@Composable
private fun HeroCard(state: DashboardUiState, targetKcal: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Brand.Purple, spotColor = Brand.Purple)
            .clip(RoundedCornerShape(32.dp))
            .background(Brand.HeroCardGradient)
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        CalorieRing(
            eatenKcal = state.eatenKcal,
            targetKcal = targetKcal,
            proteinG = state.proteinG,
            carbsG = state.carbsG,
            fatG = state.fatG,
        )
    }
}

/** Protein, carbs and fat as "eaten / target" tiles. */
@Composable
private fun MacroTiles(state: DashboardUiState) {
    val targets = state.macroTargets
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroTile("Protein", state.proteinG, targets?.proteinG, MacroColors.Protein, Modifier.weight(1f))
        MacroTile("Carbs", state.carbsG, targets?.carbsG, MacroColors.Carbs, Modifier.weight(1f))
        MacroTile("Fat", state.fatG, targets?.fatG, MacroColors.Fat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroTile(label: String, eatenG: Double, targetG: Int?, color: Color, modifier: Modifier) {
    val eaten = eatenG.roundToInt()
    StatTile(
        label = label,
        value = if (targetG != null) "$eaten" else "$eaten g",
        valueSuffix = targetG?.let { "/ $it g" },
        detail = targetG?.let { target ->
            val left = target - eaten
            if (left >= 0) "$left g left" else "${-left} g over"
        },
        color = color,
        modifier = modifier,
    )
}

/** Today's water vs. the target, with quick-add buttons. */
@Composable
private fun WaterCard(waterMl: Int, targetMl: Int?, onAdd: (Int) -> Unit) {
    val water = Brand.Sky
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(water.copy(alpha = 0.14f))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("💧 Water", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = water)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append(formatLitres(waterMl))
                        withStyle(SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(if (targetMl != null) " / ${formatLitres(targetMl)} L" else " L")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (targetMl != null) {
                    val left = targetMl - waterMl
                    Text(
                        if (left > 0) "${formatLitres(left)} L left" else "Goal reached 🎉",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Remove a glass, for mistakes.
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(water.copy(alpha = 0.2f))
                    .clickable(enabled = waterMl > 0, role = Role.Button) { onAdd(-GLASS_ML) },
                contentAlignment = Alignment.Center,
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WaterButton("+ Glass", "$GLASS_ML ml", water, Modifier.weight(1f)) { onAdd(GLASS_ML) }
            WaterButton("+ Bottle", "$BOTTLE_ML ml", water, Modifier.weight(1f)) { onAdd(BOTTLE_ML) }
        }
    }
}

private const val GLASS_ML = 250
private const val BOTTLE_ML = 500

@Composable
private fun WaterButton(label: String, amount: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dark navy reads better than white on the light sky blue.
        val textColor = Color(0xFF062A3D)
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.width(6.dp))
        Text(amount, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.75f))
    }
}

@Composable
private fun AddMealButton(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Brand.HeroGradient)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text("Add meal", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyMeals() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🍽️", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Nothing logged yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Tap Add meal to log what you ate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MealRow(meal: Meal, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(meal.mealType.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(meal.mealType.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    meal.mealType.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = meal.mealType.color,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroTag("P", meal.proteinG, MacroColors.Protein)
                    MacroTag("C", meal.carbsG, MacroColors.Carbs)
                    MacroTag("F", meal.fatG, MacroColors.Fat)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatKcal(meal.calories), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete ${meal.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MacroTag(letter: String, grams: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(
            "$letter ${formatNumber(grams)}g",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
