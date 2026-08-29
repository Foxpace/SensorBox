package com.tomasrepcik.sensorbox.di

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.tomasrepcik.sensorbox.core.error.AppFailureStore
import com.tomasrepcik.sensorbox.core.error.CompositeDiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.DiagnosticMetadata
import com.tomasrepcik.sensorbox.core.error.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.error.FileDiagnostics
import com.tomasrepcik.sensorbox.core.error.LogcatDiagnosticLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule {
    @Provides
    @Singleton
    fun provideFileDiagnostics(@ApplicationContext context: Context): FileDiagnostics = FileDiagnostics(
        context = context,
        metadata = DiagnosticMetadata(
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            buildType = if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                "debug"
            } else {
                "release"
            },
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            processName = if (Build.VERSION.SDK_INT >= 28) {
                Application.getProcessName()
            } else {
                context.applicationInfo.processName
            },
        ),
    )

    @Provides
    fun provideDiagnosticLogger(diagnostics: FileDiagnostics): DiagnosticLogger =
        CompositeDiagnosticLogger(diagnostics, LogcatDiagnosticLogger())

    @Provides
    @Singleton
    fun provideAppFailureStore(diagnosticLogger: DiagnosticLogger): AppFailureStore = AppFailureStore(diagnosticLogger)

    @Provides
    fun provideDiagnosticsStore(diagnostics: FileDiagnostics): DiagnosticsStore = diagnostics
}
