package com.veyra.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MainTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    TODAY("Today", Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard),
    NUTRITION("Nutrition", Icons.Outlined.DonutLarge, Icons.Filled.DonutLarge),
    TRAINING("Training", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    PROGRESS("Progress", Icons.Outlined.Insights, Icons.Filled.Insights),
    PROFILE("Profile", Icons.Outlined.Person, Icons.Filled.Person),
}

/** The bottom bar on the five main tabs. */
@Composable
fun MainTabsBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Column {
        RowDivider()
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { if (!isSelected) onSelect(tab) },
                    icon = { Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
