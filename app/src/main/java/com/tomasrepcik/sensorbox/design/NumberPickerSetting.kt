package com.tomasrepcik.sensorbox.design

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun NumberPickerSetting(
    title: String,
    value: Int,
    valueLabel: String,
    minimum: Int,
    maximum: Int,
    onValue: (Int) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    NumberPickerRow(title, value, valueLabel, onClick = { showPicker = true })
    SensorBoxSettingsDivider()
    if (showPicker) {
        NumberPickerDialog(
            title = title,
            value = value,
            minimum = minimum,
            maximum = maximum,
            onValue = onValue,
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun NumberPickerRow(title: String, value: Int, valueLabel: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onClick) { Text(value.toString()) }
    }
}

@Composable
private fun NumberPickerDialog(
    title: String,
    value: Int,
    minimum: Int,
    maximum: Int,
    onValue: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedValue by remember(value, minimum, maximum) {
        mutableIntStateOf(value.coerceIn(minimum, maximum))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NumberPickerView(selectedValue, minimum, maximum) { selectedValue = it } },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onValue(selectedValue)
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun NumberPickerView(value: Int, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = minimum
                    maxValue = maximum
                    wrapSelectorWheel = false
                    this.value = value
                    setOnValueChangedListener { _, _, newValue -> onValue(newValue) }
                }
            },
            update = { picker -> if (picker.value != value) picker.value = value },
        )
    }
}
