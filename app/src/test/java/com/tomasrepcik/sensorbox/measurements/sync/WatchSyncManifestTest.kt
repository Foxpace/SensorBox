package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.wearoslib.sync.WearFilePathCodec
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class WatchSyncManifestTest {
    @Test
    fun `Given the watch transfer path When the phone registers its listener Then the paths match`() {
        // Given
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))
        val filters = manifest.getElementsByTagName("intent-filter")

        // When
        val channelFilter = (0 until filters.length).map { filters.item(it) }
            .first { filter ->
                (0 until filter.childNodes.length).any { index ->
                    filter.childNodes.item(index).attributes?.getNamedItem("android:name")?.nodeValue ==
                        "com.google.android.gms.wearable.CHANNEL_EVENT"
                }
            }
        val registeredPath = (0 until channelFilter.childNodes.length)
            .mapNotNull { index ->
                channelFilter.childNodes.item(index).attributes?.getNamedItem("android:pathPrefix")?.nodeValue
            }.single()

        // Then
        assertEquals(WearFilePathCodec.PREFIX, registeredPath)
    }
}
