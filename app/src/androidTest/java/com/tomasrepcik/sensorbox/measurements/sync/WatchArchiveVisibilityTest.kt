package com.tomasrepcik.sensorbox.measurements.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tomasrepcik.sensorbox.measurements.storage.AndroidMeasurementRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchArchiveVisibilityTest {
    @Test
    fun givenWatchFoldersInArchiveWhenMeasurementsLoadThenEveryWatchFolderIsListed() = runBlocking {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folders = selectedArchive(context).listFiles()
            .filter { it.isDirectory && it.name.orEmpty().startsWith("WEAR_") }
            .mapNotNull { it.name }

        // When
        val measurements = AndroidMeasurementRepository(context).loadMeasurements().getOrThrow()

        // Then
        assertTrue(measurements.map { it.id }.containsAll(folders))
    }
}
