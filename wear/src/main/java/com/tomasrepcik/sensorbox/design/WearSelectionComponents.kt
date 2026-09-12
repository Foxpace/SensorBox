package com.tomasrepcik.sensorbox.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.CheckboxButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

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
                Icon(painterResource(it), null, tint = MaterialTheme.colorScheme.primary)
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
                Icon(painterResource(it), null, tint = MaterialTheme.colorScheme.primary)
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
