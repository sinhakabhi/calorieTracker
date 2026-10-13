package com.example.calorietracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.calorietracker.R
import com.example.calorietracker.account.GoogleAuth
import com.example.calorietracker.account.SignInResult
import com.example.calorietracker.ui.components.IconBadge
import com.example.calorietracker.ui.components.PrimaryButton
import com.example.calorietracker.ui.components.Spacing
import kotlinx.coroutines.launch

/** First screen: sign in with Google, or continue as a guest. */
@Composable
fun LoginScreen(
    googleAuth: GoogleAuth,
    onSignedIn: (name: String, email: String) -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            // The app icon's ring on its dark tile.
            Box(
                Modifier.size(72.dp).background(Color(0xFF131318), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(108.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("Calorie Tracker", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Track what you eat, drink and train. AI does the busywork; you stay in control.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Feature(Icons.Outlined.Restaurant, "Log meals in seconds", "Describe a meal and get calories and macros you can edit.")
            Feature(Icons.Outlined.FitnessCenter, "Training that fits your week", "Build a weekly plan, or let AI draft one, and check it off.")
            Feature(Icons.Outlined.Insights, "See your progress", "Calories, water and weight over time.")

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))
            error?.let {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            // Without a Google client ID in this build, skip straight to using the app.
            if (!googleAuth.isConfigured) {
                PrimaryButton(text = "Get started", onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your data stays on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }
            PrimaryButton(
                text = if (loading) "Signing in…" else "Continue with Google",
                icon = Icons.Outlined.AccountCircle,
                loading = loading,
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        when (val result = googleAuth.signIn(context)) {
                            is SignInResult.Success -> onSignedIn(result.name, result.email)
                            is SignInResult.Error -> error = result.message
                            SignInResult.Cancelled -> Unit
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onContinueAsGuest, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Continue without an account")
            }
            Text(
                "Your data stays on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        IconBadge(icon, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
