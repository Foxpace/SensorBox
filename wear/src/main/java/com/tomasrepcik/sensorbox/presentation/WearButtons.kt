package com.tomasrepcik.sensorbox.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
internal fun TransformingLazyColumnItemScope.WearPrimaryButton(
    label: String,
    transformation: TransformationSpec,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    ) {
        Text(label)
    }
}

@Composable
internal fun TransformingLazyColumnItemScope.WearSecondaryButton(
    label: String,
    transformation: TransformationSpec,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 36.dp)
            .fillMaxWidth()
            .transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    ) {
        Text(text = label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
internal fun WearPrimaryEdgeButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    EdgeButton(onClick = onClick, enabled = enabled, buttonSize = EdgeButtonSize.Small) {
        Text(label)
    }
}
