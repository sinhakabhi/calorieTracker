package com.example.calorietracker.ui.screens

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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.data.MealType
import com.example.calorietracker.network.AiGuardrails
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.OptionCard
import com.example.calorietracker.ui.components.PrimaryButton
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.components.icon
import com.example.calorietracker.viewmodel.AddMealViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(viewModel: AddMealViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (state.isEditing) "Edit meal" else "Log meal", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                PrimaryButton(
                    text = if (state.isEditing) "Save changes" else "Save meal",
                    onClick = { viewModel.save(onDone) },
                    enabled = state.canSave,
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
                .padding(horizontal = Spacing.page, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            SectionCard(
                title = "Estimate with AI",
                subtitle = "Describe what you ate; the details below are filled in for you to review.",
                icon = Icons.Outlined.AutoAwesome,
            ) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { v -> viewModel.update { it.copy(description = v.take(AiGuardrails.MAX_INPUT_CHARS)) } },
                    placeholder = { Text("e.g. 2 rotis, a bowl of dal, cucumber salad") },
                    supportingText = { Text("${state.description.length} / ${AiGuardrails.MAX_INPUT_CHARS}") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = viewModel::estimate,
                    enabled = state.description.isNotBlank() && !state.isEstimating,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(if (state.isEstimating) "Estimating…" else "Estimate", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Only the text above is sent to the AI service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.error?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            SectionHeader("Meal")
            // 2 × 2 grid so the labels never get squeezed.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.chunked(2).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { type ->
                            val selected = state.mealType == type
                            OptionCard(
                                selected = selected,
                                onClick = { viewModel.update { it.copy(mealType = type) } },
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        type.icon,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader("Details")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.calories,
                        onValueChange = { v -> viewModel.update { it.copy(calories = v) } },
                        label = { Text("Calories") },
                        suffix = { Text("kcal") },
                        isError = state.calories.isNotBlank() && state.caloriesValue == null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroField("Protein", state.protein, state.proteinValue == null, Modifier.weight(1f)) { v ->
                            viewModel.update { it.copy(protein = v) }
                        }
                        MacroField("Carbs", state.carbs, state.carbsValue == null, Modifier.weight(1f)) { v ->
                            viewModel.update { it.copy(carbs = v) }
                        }
                        MacroField("Fat", state.fat, state.fatValue == null, Modifier.weight(1f)) { v ->
                            viewModel.update { it.copy(fat = v) }
                        }
                    }
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                        label = { Text("Notes / assumed portions") },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MacroField(
    label: String,
    value: String,
    isError: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text("g") },
        isError = isError,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
