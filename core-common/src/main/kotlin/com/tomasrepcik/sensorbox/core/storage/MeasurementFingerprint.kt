package com.tomasrepcik.sensorbox.core.storage

import java.io.InputStream
import java.security.MessageDigest

fun measurementFingerprint(files: List<Pair<String, () -> InputStream>>): String {
    val measurement = MessageDigest.getInstance("SHA-256")
    files.sortedBy { it.first }.forEach { (name, open) ->
        val contents = MessageDigest.getInstance("SHA-256")
        open().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var count = input.read(buffer)
            while (count >= 0) {
                if (count > 0) contents.update(buffer, 0, count)
                count = input.read(buffer)
            }
        }
        measurement.update(name.encodeToByteArray())
        measurement.update(0.toByte())
        measurement.update(contents.digest())
    }
    return measurement.digest().joinToString("") { byte -> "%02x".format(byte) }
}
