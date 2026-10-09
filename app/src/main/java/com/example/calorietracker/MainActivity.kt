package com.example.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.calorietracker.ui.AppNavigation
import com.example.calorietracker.ui.theme.CalorieTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CalorieTrackerApp
        setContent {
            CalorieTrackerTheme {
                AppNavigation(
                    repository = app.repository,
                    geminiClient = app.geminiClient,
                )
            }
        }
    }
}
