package com.tomasrepcik.sensorbox.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.tomasrepcik.sensorbox.R

@PreviewTest
@Preview(widthDp = 320, heightDp = 180, backgroundColor = 0xFFF2F2F2, showBackground = true)
@Composable
fun launcherIconShapes() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherIcon(RoundedCornerShape(22.dp))
        LauncherIcon(CircleShape)
    }
}

@PreviewTest
@SplashPreview
@Composable
fun lightSplashScreen() = SplashScreen(Color.White)

@PreviewTest
@SplashPreview
@Composable
fun darkSplashScreen() = SplashScreen(Color.Black)

@Composable
private fun LauncherIcon(shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier.size(108.dp).background(Color.Black, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sensorbox_logo),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun SplashScreen(background: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sensorbox_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
    }
}

@Preview(widthDp = 360, heightDp = 640)
private annotation class SplashPreview
