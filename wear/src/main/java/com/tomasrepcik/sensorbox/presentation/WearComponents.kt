package com.tomasrepcik.sensorbox.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
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

@Composable
internal fun TransformingLazyColumnItemScope.WearNavigationRow(
    label: String,
    transformation: TransformationSpec,
    @DrawableRes icon: Int? = null,
    detail: String? = null,
    onClick: () -> Unit,
) {
    ChildButton(
        label = { Text(label) },
        secondaryLabel = detail?.let { { Text(it) } },
        icon = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

@Composable
internal fun TransformingLazyColumnItemScope.WearCheckboxRow(
    label: String,
    checked: Boolean,
    transformation: TransformationSpec,
    @DrawableRes icon: Int? = null,
    detail: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckboxButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(label) },
        secondaryLabel = detail?.let { { Text(it) } },
        icon = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        colors = CheckboxButtonDefaults.checkboxButtonColors(
            checkedContainerColor = Color.Transparent,
            uncheckedContainerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

@Composable
internal fun TransformingLazyColumnItemScope.WearRadioRow(
    label: String,
    selected: Boolean,
    transformation: TransformationSpec,
    onSelect: () -> Unit,
) {
    RadioButton(
        selected = selected,
        onSelect = onSelect,
        label = { Text(label) },
        colors = RadioButtonDefaults.radioButtonColors(
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

@Composable
internal fun TransformingLazyColumnItemScope.WearSwitchRow(
    label: String,
    checked: Boolean,
    transformation: TransformationSpec,
    detail: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = { Text(label) },
        secondaryLabel = detail?.let { { Text(it) } },
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = Color.Transparent,
            uncheckedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

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
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun WearPrimaryEdgeButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    EdgeButton(
        onClick = onClick,
        enabled = enabled,
        buttonSize = EdgeButtonSize.Small,
    ) {
        Text(label)
    }
}
