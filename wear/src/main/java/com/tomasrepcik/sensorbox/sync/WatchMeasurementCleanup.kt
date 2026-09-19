package com.tomasrepcik.sensorbox.sync

import com.tomasrepcik.sensorbox.core.storage.measurementFingerprint
import java.io.File
import java.util.Properties

/** Remembers a committed phone copy before deleting any watch file. */
internal class WatchMeasurementCleanup {
    fun isConfirmed(folder: File): Boolean = File(folder, RECEIPT).isFile

    fun confirm(folder: File, files: List<File>) {
        val fingerprints = Properties()
        files.forEach { file ->
            fingerprints.setProperty(file.name, measurementFingerprint(listOf(file.name to file::inputStream)))
        }
        val temporary = File(folder, "$RECEIPT.tmp")
        temporary.outputStream().use { output ->
            fingerprints.store(output, null)
            output.fd.sync()
        }
        check(temporary.renameTo(File(folder, RECEIPT))) { "Could not save watch cleanup receipt" }
    }

    fun finish(folder: File) {
        val receipt = File(folder, RECEIPT)
        val fingerprints = Properties().apply { receipt.inputStream().use(::load) }
        fingerprints.stringPropertyNames().forEach { name ->
            val expected = fingerprints.getProperty(name)
            check(name == File(name).name && name != "." && name != "..") { "Invalid watch cleanup receipt" }
            val file = File(folder, name)
            if (file.exists()) {
                check(measurementFingerprint(listOf(name to file::inputStream)) == expected) {
                    "Confirmed watch file changed before cleanup"
                }
                check(file.delete()) { "Could not delete confirmed file $name" }
            }
        }
        check(receipt.delete()) { "Could not delete watch cleanup receipt" }
        if (folder.listFiles()?.isEmpty() == true) {
            check(folder.delete()) { "Could not delete confirmed measurement folder" }
        }
    }

    private companion object {
        const val RECEIPT = ".sensorbox-confirmed.txt"
    }
}
