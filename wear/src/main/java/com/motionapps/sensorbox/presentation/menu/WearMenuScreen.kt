package com.motionapps.sensorbox.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.motionapps.sensorbox.R

@Composable
fun WearMenuScreen(state: WearMenuState, onDestinationSelected: (WearMenuDestination) -> Unit) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(scrollState = columnState) { contentPadding ->
            TransformingLazyColumn(
                state = columnState,
                contentPadding = contentPadding,
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(stringResource(R.string.app_name))
                    }
                }
                state.destinations.forEach { destination ->
                    item { WearMenuButton(destination, transformationSpec, onDestinationSelected) }
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.WearMenuButton(
    destination: WearMenuDestination,
    transformationSpec: TransformationSpec,
    onDestinationSelected: (WearMenuDestination) -> Unit,
) {
    val label = stringResource(destination.labelResource)
    Button(
        label = { Text(label) },
        icon = {
            Image(
                painter = painterResource(destination.iconResource),
                contentDescription = null,
            )
        },
        onClick = { onDestinationSelected(destination) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
    )
}
