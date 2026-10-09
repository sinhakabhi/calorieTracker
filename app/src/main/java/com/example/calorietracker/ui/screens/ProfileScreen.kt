package com.example.calorietracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.domain.ActivityLevel
import com.example.calorietracker.domain.CalorieCalculator
import com.example.calorietracker.domain.Goal
import com.example.calorietracker.domain.Sex
import com.example.calorietracker.network.AiGuardrails
import com.example.calorietracker.ui.components.GradientButton
import com.example.calorietracker.ui.components.OptionCard
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.formatKcal
import com.example.calorietracker.ui.formatLitres
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.ui.theme.Brand
import com.example.calorietracker.viewmodel.ProfileAssistState
import com.example.calorietracker.viewmodel.ProfileForm
import com.example.calorietracker.viewmodel.ProfileViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, isOnboarding: Boolean, onDone: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()
    val assist by viewModel.assist.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOnboarding) "" else "Your profile") },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(onClick = onDone) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                GradientButton(
                    text = if (isOnboarding) "Get started" else "Save",
                    onClick = { viewModel.save(onDone) },
                    enabled = form.isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isOnboarding) {
                Column {
                    Text("Let's get to know you", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "We use this to work out how many calories you need each day.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AssistCard(
                state = assist,
                onTextChange = viewModel::updateAssistText,
                onFill = viewModel::fillFromDescription,
            )

            SectionCard(title = "About you") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Sex.entries.forEach { sex ->
                        OptionCard(
                            selected = form.sex == sex,
                            onClick = { viewModel.update { it.copy(sex = sex) } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                sex.label,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                NumberField(
                    label = "Age",
                    suffix = "years",
                    value = form.age,
                    onValueChange = { v -> viewModel.update { it.copy(age = v) } },
                    isError = form.age.isNotBlank() && !form.ageValid,
                    errorText = "Enter an age from 13 to 100",
                    decimal = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Height",
                        suffix = "cm",
                        value = form.heightCm,
                        onValueChange = { v -> viewModel.update { it.copy(heightCm = v) } },
                        isError = form.heightCm.isNotBlank() && !form.heightValid,
                        errorText = "100–250 cm",
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        label = if (isOnboarding) "Weight" else "Today's weight",
                        suffix = "kg",
                        value = form.weightKg,
                        onValueChange = { v -> viewModel.update { it.copy(weightKg = v) } },
                        isError = form.weightKg.isNotBlank() && !form.weightValid,
                        errorText = "30–300 kg",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!isOnboarding) {
                    Text(
                        "Saving logs this weight for today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard(
                title = "How active are you?",
                subtitle = "Your resting burn is multiplied by this number to estimate what you burn in a full day.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityLevel.entries.forEach { level ->
                        OptionCard(
                            selected = form.activity == level,
                            onClick = { viewModel.update { it.copy(activity = level) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(level.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text("×${level.factor}", style = MaterialTheme.typography.labelLarge)
                            }
                            Text(level.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            SectionCard(title = "Your goal") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { goal ->
                        OptionCard(
                            selected = form.goal == goal,
                            onClick = { viewModel.update { it.copy(goal = goal) } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(goal.label, style = MaterialTheme.typography.titleSmall)
                                Text(goalAdjustmentText(goal), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            TargetBreakdownCard(form)

            if (!isOnboarding && weightHistory.isNotEmpty()) {
                SectionCard(title = "Weight history") {
                    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                    weightHistory.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(LocalDate.ofEpochDay(entry.epochDay).format(formatter), modifier = Modifier.weight(1f))
                            Text("${formatNumber(entry.weightKg)} kg", fontWeight = FontWeight.SemiBold)
                            // Keep at least one entry: the daily target needs a weight.
                            IconButton(onClick = { viewModel.deleteWeight(entry) }, enabled = weightHistory.size > 1) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete weight entry")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Lets the user describe themselves in one sentence and have Gemini fill in the form. */
@Composable
private fun AssistCard(state: ProfileAssistState, onTextChange: (String) -> Unit, onFill: () -> Unit) {
    // A soft violet → pink wash so the AI card stands out from the plain form cards.
    val wash = Brush.linearGradient(listOf(Brand.Violet.copy(alpha = 0.22f), Brand.Pink.copy(alpha = 0.18f)))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .background(wash),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("✨ Describe yourself", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "AI will fill in the form for you. You can still edit everything below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = state.text,
                onValueChange = { onTextChange(it.take(AiGuardrails.MAX_INPUT_CHARS)) },
                supportingText = { Text("${state.text.length} / ${AiGuardrails.MAX_INPUT_CHARS}") },
                placeholder = {
                    Text("e.g. I'm a 27M, I work out 3–4 times a week, I weigh 100 kg, I'm 182 cm and want to get to 90 kg")
                },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            GradientButton(
                text = if (state.isLoading) "Reading…" else "Auto-fill with AI",
                onClick = onFill,
                enabled = state.text.isNotBlank() || state.isLoading,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Only the text above is sent to the AI service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.message?.let { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun goalAdjustmentText(goal: Goal): String = when {
    goal.adjustmentKcal > 0 -> "+${goal.adjustmentKcal} kcal"
    goal.adjustmentKcal < 0 -> "−${-goal.adjustmentKcal} kcal"
    else -> "±0 kcal"
}

/** Shows how the target is worked out: resting burn × activity ± goal. */
@Composable
private fun TargetBreakdownCard(form: ProfileForm) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brand.HeroGradient),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your daily target", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
            val bmr = form.previewBmr
            val target = form.previewTarget
            val macros = form.previewMacros
            if (bmr == null || target == null || macros == null) {
                Text("Fill in your age, height and weight to see it.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                return@Column
            }
            Text(
                "${formatKcal(target)} kcal",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            BreakdownRow("Resting burn (BMR)", "${formatKcal(bmr)} kcal")
            BreakdownRow("× ${form.activity.label}", "×${form.activity.factor}")
            BreakdownRow("${form.goal.label} weight", goalAdjustmentText(form.goal))
            if (target == CalorieCalculator.MIN_DAILY_KCAL) {
                Text(
                    "Raised to the ${formatKcal(CalorieCalculator.MIN_DAILY_KCAL)} kcal safe minimum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroTargetChip("Protein", macros.proteinG, Modifier.weight(1f))
                MacroTargetChip("Carbs", macros.carbsG, Modifier.weight(1f))
                MacroTargetChip("Fat", macros.fatG, Modifier.weight(1f))
            }
            Text(
                "Protein ${formatNumber(form.activity.proteinPerKg)} g per kg of body weight · " +
                    "fat ${(CalorieCalculator.FAT_SHARE * 100).toInt()}% of calories · carbs the rest.\n" +
                    "Water ${formatLitres(CalorieCalculator.waterTargetMl(form.weightValue!!))} L a day (35 ml per kg).",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun MacroTargetChip(label: String, grams: Int, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$grams g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun NumberField(
    label: String,
    suffix: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorText: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    decimal: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = isError,
        supportingText = if (isError) ({ Text(errorText) }) else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}
