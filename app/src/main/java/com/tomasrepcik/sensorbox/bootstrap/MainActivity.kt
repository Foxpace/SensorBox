package com.tomasrepcik.sensorbox.bootstrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tomasrepcik.sensorbox.navigation.SensorBoxRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorBoxRoot(
                showPrivacyRationale = intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE,
            )
        }
    }
}
