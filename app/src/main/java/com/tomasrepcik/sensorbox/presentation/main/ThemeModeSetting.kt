package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode

@Composable
internal fun ThemeModeSetting(selected: AppThemeMode, onIntent: (SettingsIntent) -> Unit) {
    SettingsChoiceSetting(
        title = stringResource(R.string.theme_mode),
        description = stringResource(R.string.theme_mode_description),
        options = listOf(
            AppThemeMode.AUTOMATIC to stringResource(R.string.theme_automatic),
            AppThemeMode.LIGHT to stringResource(R.string.theme_light),
            AppThemeMode.DARK to stringResource(R.string.theme_dark),
        ),
        selected = selected,
    ) { onIntent(SettingsIntent.SetThemeMode(it)) }
}
