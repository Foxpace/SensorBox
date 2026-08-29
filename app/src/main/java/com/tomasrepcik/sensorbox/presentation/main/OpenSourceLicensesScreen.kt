package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.licenses.OpenSourceLicense

@Composable
fun OpenSourceLicensesScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLicense = state.selectedLicenseName?.let { name ->
        state.openSourceLicenses.firstOrNull { it.name == name }
    }
    SensorBoxBackScreen(
        title = stringResource(R.string.about_licenses),
        onBack = onBack,
        modifier = modifier,
    ) {
        items(state.openSourceLicenses, key = OpenSourceLicense::name) { license ->
            OpenSourceLicenseRow(license.name) { onIntent(SettingsIntent.SelectOpenSourceLicense(license.name)) }
        }
    }
    selectedLicense?.let { license ->
        OpenSourceLicenseDialog(license) { onIntent(SettingsIntent.DismissOpenSourceLicense) }
    }
}
