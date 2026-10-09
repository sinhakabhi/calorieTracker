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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.data.MealType
import com.example.calorietracker.network.AiGuardrails
import com.example.calorietracker.ui.components.GradientButton
import com.example.calorietracker.ui.components.OptionCard
import com.example.calorietracker.ui.components.SectionCard
import com.example.calorietracker.viewmodel.AddMealViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(viewModel: AddMealViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit meal" else "Add meal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                GradientButton(
                    text = if (state.isEditing) "Save changes" else "Save meal",
                    onClick = { viewModel.save(onDone) },
                    enabled = state.canSave,
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
            SectionCard(
                title = "What did you eat?",
                subtitle = "Describe it in your own words and let AI estimate the numbers.",
            ) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { v -> viewModel.update { it.copy(description = v.take(AiGuardrails.MAX_INPUT_CHARS)) } },
                    placeholder = { Text("e.g. 2 rotis, a bowl of dal, cucumber salad") },
                    supportingText = { Text("${state.description.length} / ${AiGuardrails.MAX_INPUT_CHARS}") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = if (state.isEstimating) "Estimating…" else "✨  Estimate with AI",
                    onClick = viewModel::estimate,
                    enabled = state.description.isNotBlank() || state.isEstimating,
                    loading = state.isEstimating,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Only the text above is sent to the AI service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.error?.let { message ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(message, modifier = Modifier.padding(16.dp))
                }
            }

            SectionCard(title = "Meal type") {
                // 2 × 2 grid so the labels never get squeezed.
                MealType.entries.chunked(2).forEachIndexed { rowIndex, rowTypes ->
                    if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTypes.forEach { type ->
                            OptionCard(
                                selected = state.mealType == type,
                                onClick = { viewModel.update { it.copy(mealType = type) } },
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(type.emoji, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text(type.label, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Details", subtitle = "Check or adjust the numbers before saving.") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.calories,
                        onValueChange = { v -> viewModel.update { it.copy(calories = v) } },
                        label = { Text("Calories") },
                        suffix = { Text("kcal") },
                        isError = state.calories.isNotBlank() && state.caloriesValue == null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
