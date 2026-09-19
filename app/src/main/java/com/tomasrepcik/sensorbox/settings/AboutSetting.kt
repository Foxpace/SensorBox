package com.tomasrepcik.sensorbox.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.about.AboutDialog
import com.tomasrepcik.sensorbox.navigation.MainRoute

@Composable
internal fun AboutSetting(onIntent: (SettingsIntent) -> Unit) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    SettingsControlRow(
        title = stringResource(R.string.menu_about),
        description = stringResource(R.string.about_summary),
        showDivider = false,
        showChevron = true,
    ) { showAboutDialog = true }
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onPrivacy = {
                showAboutDialog = false
                onIntent(SettingsIntent.Navigate(MainRoute.PRIVACY))
            },
            onLicenses = {
                showAboutDialog = false
                onIntent(SettingsIntent.Navigate(MainRoute.LICENSES))
            },
        )
    }
}
