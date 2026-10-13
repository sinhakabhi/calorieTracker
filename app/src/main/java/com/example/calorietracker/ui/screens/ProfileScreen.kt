package com.example.calorietracker.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calorietracker.BuildConfig
import com.example.calorietracker.network.AiRateLimiter
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.MacroColors
import com.example.calorietracker.ui.components.MainTab
import com.example.calorietracker.ui.components.MainTabsBar
import com.example.calorietracker.ui.components.RowDivider
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.components.StatBlock
import com.example.calorietracker.ui.formatKcal
import com.example.calorietracker.ui.formatLitres
import com.example.calorietracker.ui.formatNumber
import com.example.calorietracker.ui.theme.Accents
import com.example.calorietracker.ui.theme.tabular
import com.example.calorietracker.viewmodel.ProfileOverviewViewModel

/** The Profile tab: who you are, your targets, your body & goal, and settings. */
@Composable
fun ProfileScreen(
    viewModel: ProfileOverviewViewModel,
    onSelectTab: (MainTab) -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    /** Null when Google sign-in isn't configured in this build. */
    onSignIn: (() -> Unit)?,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(bottomBar = { MainTabsBar(MainTab.PROFILE, onSelectTab) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            item { ScreenTitle(title = "Profile", subtitle = "Account and settings") }

            // Account
            item {
                val account = state.account
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                account?.initials?.takeIf { account.name.isNotBlank() } ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                account?.name?.takeIf { it.isNotBlank() } ?: "Guest",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                account?.email ?: if (onSignIn != null) "Not signed in" else "Data stored on this phone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (account?.isGuest != false && onSignIn != null) {
                            TextButton(onClick = onSignIn) { Text("Sign in") }
                        }
                    }
                }
            }

            // Targets
            item { SectionHeader("Daily targets") }
            item {
                val targets = state.targets
                AppCard {
                    StatBlock(
                        label = "Calories",
                        value = targets?.let { formatKcal(it.kcal) } ?: "—",
                        valueSuffix = "kcal per day",
                    )
                    Spacer(Modifier.height(14.dp))
                    RowDivider()
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBlock("Protein", targets?.macros?.proteinG?.toString() ?: "—", valueSuffix = "g", color = MacroColors.Protein, modifier = Modifier.weight(1f))
                        StatBlock("Carbs", targets?.macros?.carbsG?.toString() ?: "—", valueSuffix = "g", color = MacroColors.Carbs, modifier = Modifier.weight(1f))
                        StatBlock("Fat", targets?.macros?.fatG?.toString() ?: "—", valueSuffix = "g", color = MacroColors.Fat, modifier = Modifier.weight(1f))
                        StatBlock("Water", targets?.let { formatLitres(it.waterMl) } ?: "—", valueSuffix = "L", color = Accents.Water, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Body & goal
            item {
                SectionHeader("Body & goal") {
                    TextButton(onClick = onEditProfile) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            }
            item {
                val profile = state.profile
                AppCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                    val rows = listOf(
                        "Age" to (profile?.let { "${it.age} years" } ?: "—"),
                        "Sex" to (profile?.sex?.label ?: "—"),
                        "Height" to (profile?.let { "${formatNumber(it.heightCm)} cm" } ?: "—"),
                        "Weight" to (state.latestWeight?.let { "${formatNumber(it.weightKg)} kg" } ?: "—"),
                        "Activity" to (profile?.activityLevel?.label ?: "—"),
                        "Goal" to (profile?.goal?.let { "${it.label} weight" } ?: "—"),
                    )
                    rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) RowDivider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(value, style = MaterialTheme.typography.bodyMedium.tabular())
                        }
                    }
                }
            }

            // Settings
            item { SectionHeader("Settings") }
            item {
                val r = state.reminders
                val on = listOfNotNull("Water".takeIf { r.water }, "Meals".takeIf { r.meals }, "Workouts".takeIf { r.workout })
                AppCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = if (on.isEmpty()) "Off" else on.joinToString(", "),
                        onClick = onOpenNotifications,
                    )
                    RowDivider(Modifier.padding(start = 68.dp))
                    SettingsRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "AI usage today",
                        subtitle = "${state.aiRequestsUsed} of ${AiRateLimiter.PER_DAY} requests · resets at midnight",
                    )
                    RowDivider(Modifier.padding(start = 68.dp))
                    SettingsRow(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy",
                        subtitle = "Your data stays on this phone. Only text you send to AI leaves it.",
                    )
                }
            }

            if (state.account?.isGuest == false) {
                item {
                    OutlinedButton(
                        onClick = { confirmSignOut = true },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                Text(
                    "Calorie Tracker ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your meals, plan and history stay on this phone and will be here when you sign back in.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    val content = @Composable {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary, size = 36.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, color = Color.Transparent) { content() } else content()
}
