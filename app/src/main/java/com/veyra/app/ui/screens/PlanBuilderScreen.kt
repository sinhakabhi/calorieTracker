package com.veyra.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veyra.app.network.AiGuardrails
import com.veyra.app.ui.components.AppCard
import com.veyra.app.ui.components.PrimaryButton
import com.veyra.app.ui.components.SectionCard
import com.veyra.app.ui.components.Spacing
import com.veyra.app.viewmodel.PlanBuilderViewModel

/** Tap-to-fill examples covering different kinds of training. */
private val EXAMPLES = listOf(
    "Gym 4 days a week to get lean",
    "Bulk up, 5 days a week, full gym",
    "First Hyrox in 10 weeks, 5 days a week",
    "Run a 10K in 8 weeks, 3–4 runs a week",
    "Home workouts, no equipment, 30 min a day",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanBuilderScreen(viewModel: PlanBuilderViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New training plan", style = MaterialTheme.typography.titleMedium) },
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
                    text = if (state.isLoading) "Building your week…" else "Create plan",
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = { viewModel.generate(onDone) },
                    enabled = state.goal.isNotBlank(),
                    loading = state.isLoading,
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
                title = "What are you training for?",
                subtitle = "Your goal, how many days you can train, your equipment, and any injuries. " +
                    "AI builds a weekly plan you can edit.",
                icon = Icons.Outlined.AutoAwesome,
            ) {
                OutlinedTextField(
                    value = state.goal,
                    onValueChange = { viewModel.updateGoal(it.take(AiGuardrails.MAX_WORKOUT_CHARS)) },
                    placeholder = { Text("e.g. I go to the gym and want to get lean. I can train 4 days a week, about an hour each.") },
                    supportingText = { Text("${state.goal.length} / ${AiGuardrails.MAX_WORKOUT_CHARS}") },
                    minLines = 4,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Examples", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EXAMPLES.forEach { example ->
                        SuggestionChip(
                            onClick = { viewModel.updateGoal(example) },
                            label = { Text(example) },
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }

            state.error?.let { message ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.hasExistingPlan) {
                AppCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "This replaces your current plan and clears its check-offs.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Text(
                "Your goal and basic profile (age, sex, height, weight, activity level, goal) are sent to the AI " +
                    "service to tailor the plan. This isn't medical advice: if you have an injury or condition, " +
                    "check with a doctor or physiotherapist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
