package com.tomasrepcik.sensorbox.measurements.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.ActivityTransition
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxPanel
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.SensorSeriesSample
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

internal fun LazyListScope.recordedEventItems(content: MeasurementFileContent.SensorSeries) {
    val kind = checkNotNull(recordedEventKind(content.columns))
    item { RecordedEventSummary(content) }
    itemsIndexed(content.samples) { index, sample ->
        when (kind) {
            RecordedEventKind.ACTIVITY_TRANSITION -> ActivityTransitionRow(content.columns, sample)
            RecordedEventKind.STEP -> RecordedEventRow(sample, index, isStep = true)
            RecordedEventKind.MOTION -> RecordedEventRow(sample, index, isStep = false)
        }
    }
}

@Composable
internal fun RecordedEventSummary(content: MeasurementFileContent.SensorSeries) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val summary = when {
            content.samples.isEmpty() -> stringResource(R.string.preview_no_events)
            content.truncated -> stringResource(R.string.preview_shown_event_count, content.samples.size)
            else -> stringResource(R.string.preview_event_count, content.samples.size)
        }
        Text(summary, style = MaterialTheme.typography.titleMedium)
        if (content.truncated) Text(stringResource(R.string.preview_sampled_events_notice))
    }
}

@Composable
internal fun RecordedEventRow(sample: SensorSeriesSample, index: Int, isStep: Boolean) {
    EventPanel(sample.timestampMillis) {
        val title = if (isStep) {
            stringResource(R.string.preview_step_number, index + 1)
        } else {
            stringResource(R.string.preview_motion_detected)
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ActivityTransitionRow(columns: List<String>, sample: SensorSeriesSample) {
    val activity = sample.values.getOrNull(columns.indexOf("activity"))
    val transition = sample.values.getOrNull(columns.indexOf("enter_exit"))
    val activityName = stringResource(activityNameResource(activity))
    val title = when (transition) {
        ActivityTransition.ACTIVITY_TRANSITION_ENTER.toDouble() ->
            stringResource(R.string.preview_activity_started, activityName)

        ActivityTransition.ACTIVITY_TRANSITION_EXIT.toDouble() ->
            stringResource(R.string.preview_activity_ended, activityName)

        else -> stringResource(R.string.preview_unknown_transition, activityName, transition.toString())
    }
    EventPanel(sample.timestampMillis) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EventPanel(timestampMillis: Long, content: @Composable () -> Unit) {
    SensorBoxPanel(Modifier.padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", LocalLocale.current.platformLocale).format(Date(timestampMillis)),
                style = MaterialTheme.typography.titleSmall,
            )
            content()
        }
    }
}
