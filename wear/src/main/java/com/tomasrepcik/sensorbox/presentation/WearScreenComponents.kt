package com.tomasrepcik.sensorbox.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
internal fun WearListScreen(
    edgeButton: (@Composable () -> Unit)? = null,
    content: TransformingLazyColumnScope.(TransformationSpec) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformation = rememberTransformationSpec()
    AppScaffold {
        if (edgeButton == null) {
            ScreenScaffold(scrollState = listState) { padding ->
                TransformingLazyColumn(state = listState, contentPadding = padding) {
                    content(transformation)
                }
            }
        } else {
            ScreenScaffold(
                scrollState = listState,
                edgeButton = { edgeButton() },
            ) { padding ->
                TransformingLazyColumn(state = listState, contentPadding = padding) {
                    content(transformation)
                }
            }
        }
    }
}

@Composable
internal fun TransformingLazyColumnItemScope.WearPageTitle(label: String, transformation: TransformationSpec) {
    ListHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
internal fun TransformingLazyColumnItemScope.WearSectionTitle(label: String, transformation: TransformationSpec) {
    ListHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary)
    }
}
