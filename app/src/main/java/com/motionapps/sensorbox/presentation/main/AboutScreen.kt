package com.motionapps.sensorbox.presentation.main

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.BuildConfig
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.appResult

@Composable
fun AboutDialog(onDismiss: () -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_launcher_historic_round),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        },
        title = { Text(stringResource(R.string.about_title)) },
        text = { AboutDialogBody(onPrivacy) },
        confirmButton = {
            TextButton(onClick = onLicenses) {
                Text(stringResource(R.string.about_licenses))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun AboutDialogBody(onPrivacy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.about_app_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onPrivacy, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.about_privacy_policy))
        }
    }
}

@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val licenses = remember(resources) {
        appResult(AppErrorCode.STORAGE, "Load open source licenses") {
            loadOpenSourceLicenses(resources)
        }.getOrDefault(emptyList())
    }
    var selectedLicense by remember { mutableStateOf<OpenSourceLicense?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SensorBoxTopAppBar(stringResource(R.string.about_licenses), onBack) }
        items(licenses, key = OpenSourceLicense::name) { license ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { selectedLicense = license },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = license.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    selectedLicense?.let { license ->
        AlertDialog(
            onDismissRequest = { selectedLicense = null },
            title = { Text(license.name) },
            text = {
                Box(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(license.text, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLicense = null }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

private data class OpenSourceLicense(val name: String, val text: String)

private fun loadOpenSourceLicenses(resources: Resources): List<OpenSourceLicense> {
    val licenseBytes = resources.openRawResource(R.raw.third_party_licenses).use { it.readBytes() }
    return resources.openRawResource(R.raw.third_party_license_metadata).bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            val location = line.substringBefore(' ')
            val name = line.substringAfter(' ', missingDelimiterValue = "").trim()
            val offset = location.substringBefore(':').toIntOrNull()
            val length = location.substringAfter(':', missingDelimiterValue = "").toIntOrNull()
            if (name.isEmpty() || offset == null || length == null || offset + length > licenseBytes.size) {
                return@mapNotNull null
            }
            OpenSourceLicense(name, licenseBytes.decodeToString(offset, offset + length).trim())
        }.distinctBy(OpenSourceLicense::name).sortedBy { it.name.lowercase() }.toList()
    }
}
