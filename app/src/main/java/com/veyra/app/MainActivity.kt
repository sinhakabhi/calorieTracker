package com.veyra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.veyra.app.ui.AppNavigation
import com.veyra.app.ui.theme.VeyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VeyraApp
        setContent {
            VeyraTheme {
                AppNavigation(app)
            }
        }
    }
}
