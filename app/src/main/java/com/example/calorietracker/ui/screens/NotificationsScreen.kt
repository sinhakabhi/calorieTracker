package com.example.calorietracker.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.calorietracker.notifications.Reminder
import com.example.calorietracker.notifications.ReminderSettings
import com.example.calorietracker.notifications.ReminderType
import com.example.calorietracker.notifications.Reminders
import com.example.calorietracker.ui.components.AppCard
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.RowDivider
import com.example.calorietracker.ui.components.SectionHeader
import com.example.calorietracker.ui.components.Spacing
import com.example.calorietracker.ui.theme.Accents

/** Reminder switches. Turning one on asks for notification permission (Android 13+) if needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(settings: ReminderSettings, onChange: (ReminderSettings) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    // The change waiting for the permission answer.
    var pending by remember { mutableStateOf<ReminderSettings?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        pending?.takeIf { granted }?.let(onChange)
        pending = null
    }

    fun request(newSettings: ReminderSettings) {
        val turningOn = newSettings.anyEnabled && !settings.anyEnabled ||
            (newSettings.water && !settings.water) || (newSettings.meals && !settings.meals) || (newSettings.workout && !settings.workout)
        if (turningOn && !Reminders.canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pending = newSettings
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onChange(newSettings)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notifications", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
        ) {
            Text(
                "Gentle nudges to keep your day on track. Nothing is sent before 8 AM or after 10 PM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (permissionDenied) {
                AppCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notifications are blocked", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Allow notifications for Calorie Tracker in system settings to get reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            }) { Text("Open settings") }
                        }
                    }
                }
            }

            SectionHeader("Reminders")
            AppCard(contentPadding = PaddingValues(0.dp)) {
                ToggleRow(
                    icon = Icons.Outlined.WaterDrop,
                    tint = Accents.Water,
                    title = "Water",
                    subtitle = "Every 2 hours when you're behind your daily water target",
                    checked = settings.water,
                    onChange = { request(settings.copy(water = it)) },
                )
                RowDivider(Modifier.padding(start = 68.dp))
                ToggleRow(
                    icon = Icons.Outlined.Restaurant,
                    tint = MaterialTheme.colorScheme.primary,
                    title = "Meal logging",
                    subtitle = "If breakfast, lunch or dinner isn't logged by 10 AM, 2 PM or 9 PM",
                    checked = settings.meals,
                    onChange = { request(settings.copy(meals = it)) },
                )
                RowDivider(Modifier.padding(start = 68.dp))
                ToggleRow(
                    icon = Icons.Outlined.FitnessCenter,
                    tint = MaterialTheme.colorScheme.primary,
                    title = "Workout",
                    subtitle = "At 6 PM if today's training isn't finished",
                    checked = settings.workout,
                    onChange = { request(settings.copy(workout = it)) },
                )
            }

            OutlinedButton(
                onClick = {
                    if (Reminders.canNotify(context)) {
                        Reminders.post(context, Reminder(ReminderType.WATER, "Reminders are working", "This is how your reminders will look."))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Send a test notification") }

            Text(
                "Reminders are checked about once an hour, so they can arrive a little after the times above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint, size = 36.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
