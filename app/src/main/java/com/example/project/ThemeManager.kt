package com.example.project

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    var isDarkTheme by mutableStateOf(prefs.getBoolean("is_dark_theme", false))
        private set
    
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
    }
}