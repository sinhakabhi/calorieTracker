package com.veyra.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyra.app.domain.ActivityLevel
import com.veyra.app.domain.CalorieCalculator
import com.veyra.app.domain.Goal
import com.veyra.app.domain.Sex
import com.veyra.app.network.AiGuardrails
import com.veyra.app.ui.components.AppCard
import com.veyra.app.ui.components.MacroColors
import com.veyra.app.ui.components.OptionCard
import com.veyra.app.ui.components.PrimaryButton
import com.veyra.app.ui.components.RowDivider
import com.veyra.app.ui.components.SectionCard
import com.veyra.app.ui.components.SectionHeader
import com.veyra.app.ui.components.Spacing
import com.veyra.app.ui.components.StatBlock
import com.veyra.app.ui.formatKcal
import com.veyra.app.ui.formatLitres
import com.veyra.app.ui.formatNumber
import com.veyra.app.ui.theme.Accents
import com.veyra.app.ui.theme.tabular
import com.veyra.app.viewmodel.ProfileAssistState
import com.veyra.app.viewmodel.ProfileForm
import com.veyra.app.viewmodel.ProfileViewModel

/**
 * The body & goal form, used for first-run setup ([isOnboarding]) and for editing from the
 * Profile tab. Saving calls [onDone].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: ProfileViewModel,
    isOnboarding: Boolean,
    onDone: () -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val assist by viewModel.assist.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (!isOnboarding) {
                CenterAlignedTopAppBar(
                    title = { Text("Body & goal", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PrimaryButton(
                    text = if (isOnboarding) "Get started" else "Save changes",
                    onClick = { viewModel.save(onDone) },
                    enabled = form.isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.page, vertical = 12.dp),
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
                .padding(horizontal = Spacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            if (isOnboarding) {
                ScreenTitle(title = "Set up your profile", subtitle = "Welcome")
                Text(
                    "Your details are used to work out your daily calorie, macro and water targets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AssistCard(state = assist, onTextChange = viewModel::updateAssistText, onFill = viewModel::fillFromDescription)

            SectionHeader("Body")
            AppCard {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Sex.entries.forEachIndexed { index, sex ->
                        SegmentedButton(
                            selected = form.sex == sex,
                            onClick = { viewModel.update { it.copy(sex = sex) } },
                            shape = SegmentedButtonDefaults.itemShape(index, Sex.entries.size),
                        ) { Text(sex.label) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                NumberField(
                    label = "Age",
                    suffix = "years",
                    value = form.age,
                    onValueChange = { v -> viewModel.update { it.copy(age = v) } },
                    isError = form.age.isNotBlank() && !form.ageValid,
                    errorText = "Enter an age from 13 to 100",
                    decimal = false,
                )
                Spacer(Modifier.height(10.dp))
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
                        label = "Weight",
                        suffix = "kg",
                        value = form.weightKg,
                        onValueChange = { v -> viewModel.update { it.copy(weightKg = v) } },
                        isError = form.weightKg.isNotBlank() && !form.weightValid,
                        errorText = "30–300 kg",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!isOnboarding) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Saving logs this as today's weight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionHeader("Activity level")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    OptionCard(
                        selected = form.activity == level,
                        onClick = { viewModel.update { it.copy(activity = level) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = form.activity == level, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(level.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    level.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "×${level.factor}",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Text(
                "Your resting burn is multiplied by this factor to estimate what you burn in a full day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader("Goal")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Goal.entries.forEachIndexed { index, goal ->
                    SegmentedButton(
                        selected = form.goal == goal,
                        onClick = { viewModel.update { it.copy(goal = goal) } },
                        shape = SegmentedButtonDefaults.itemShape(index, Goal.entries.size),
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(goal.label)
                                Text(
                                    goalAdjustmentText(goal),
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            SectionHeader("Daily targets")
            TargetsCard(form)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Lets the user describe themselves in one sentence and have AI fill in the form. */
@Composable
private fun AssistCard(state: ProfileAssistState, onTextChange: (String) -> Unit, onFill: () -> Unit) {
    SectionCard(
        title = "Fill in with AI",
        subtitle = "Describe yourself in a sentence; you can still edit everything below.",
        icon = Icons.Outlined.AutoAwesome,
    ) {
        OutlinedTextField(
            value = state.text,
            onValueChange = { onTextChange(it.take(AiGuardrails.MAX_INPUT_CHARS)) },
            placeholder = { Text("e.g. 27, male, 182 cm, 100 kg, gym 3–4 times a week, want to reach 90 kg") },
            supportingText = { Text("${state.text.length} / ${AiGuardrails.MAX_INPUT_CHARS}") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = onFill,
            enabled = state.text.isNotBlank() && !state.isLoading,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(if (state.isLoading) "Reading…" else "Auto-fill", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Only the text above is sent to the AI service.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.message?.let {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Accents.Success, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun goalAdjustmentText(goal: Goal): String = when {
    goal.adjustmentKcal > 0 -> "+${goal.adjustmentKcal} kcal"
    goal.adjustmentKcal < 0 -> "−${-goal.adjustmentKcal} kcal"
    else -> "±0 kcal"
}

/** The daily calorie target, how it's worked out, and the macro and water targets. */
@Composable
private fun TargetsCard(form: ProfileForm) {
    AppCard {
        val bmr = form.previewBmr
        val target = form.previewTarget
        val macros = form.previewMacros
        if (bmr == null || target == null || macros == null) {
            Text(
                "Fill in your age, height and weight to see your targets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AppCard
        }
        StatBlock(label = "Calories per day", value = formatKcal(target), valueSuffix = "kcal")
        Spacer(Modifier.height(12.dp))
        BreakdownRow("Resting burn (BMR)", "${formatKcal(bmr)} kcal")
        BreakdownRow("× ${form.activity.label}", "×${form.activity.factor}")
        BreakdownRow("${form.goal.label} weight", goalAdjustmentText(form.goal))
        if (target == CalorieCalculator.MIN_DAILY_KCAL) {
            Text(
                "Raised to the ${formatKcal(CalorieCalculator.MIN_DAILY_KCAL)} kcal safe minimum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        RowDivider()
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBlock("Protein", "${macros.proteinG}", color = MacroColors.Protein, valueSuffix = "g", modifier = Modifier.weight(1f))
            StatBlock("Carbs", "${macros.carbsG}", color = MacroColors.Carbs, valueSuffix = "g", modifier = Modifier.weight(1f))
            StatBlock("Fat", "${macros.fatG}", color = MacroColors.Fat, valueSuffix = "g", modifier = Modifier.weight(1f))
            StatBlock(
                "Water",
                formatLitres(CalorieCalculator.waterTargetMl(form.weightValue!!)),
                color = Accents.Water,
                valueSuffix = "L",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Protein ${formatNumber(form.activity.proteinPerKg)} g/kg · fat ${(CalorieCalculator.FAT_SHARE * 100).toInt()}% of calories · " +
                "carbs the rest · water 35 ml/kg",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.tabular())
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
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}
