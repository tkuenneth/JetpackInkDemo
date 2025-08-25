package dev.tkuenneth.jetpackinkdemo

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable


@Composable
fun Context.defaultColorScheme(): ColorScheme {
    return if (isSystemInDarkTheme())
        dynamicDarkColorScheme(this)
    else
        dynamicLightColorScheme(this)
}
