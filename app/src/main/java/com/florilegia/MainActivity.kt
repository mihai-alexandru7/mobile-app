package com.florilegia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.florilegia.ui.navigation.AppNavigation
import com.florilegia.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FlorilegiaApplication
        setContent {
            val darkOverride by app.container.preferencesStore.darkModeFlow
                .collectAsStateWithLifecycle(initialValue = null)
            val isDark = darkOverride ?: isSystemInDarkTheme()

            Theme(darkTheme = isDark) {
                AppNavigation(isDarkTheme = isDark)
            }
        }
    }
}