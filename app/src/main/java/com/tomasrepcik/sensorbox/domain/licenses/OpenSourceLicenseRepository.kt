package com.tomasrepcik.sensorbox.domain.licenses

import android.content.Context
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class OpenSourceLicense(val name: String, val text: String)

interface OpenSourceLicenseRepository {
    fun load(): AppResult<List<OpenSourceLicense>>
}

class AndroidOpenSourceLicenseRepository @Inject constructor(@ApplicationContext private val context: Context) :
    OpenSourceLicenseRepository {
    override fun load(): AppResult<List<OpenSourceLicense>> = appResult(
        AppErrorCode.STORAGE,
        "Load open source licenses",
    ) {
        val resources = context.resources
        val licenseBytes = resources.openRawResource(R.raw.third_party_licenses).use { it.readBytes() }
        resources.openRawResource(R.raw.third_party_license_metadata).bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val location = line.substringBefore(' ')
                val name = line.substringAfter(' ', missingDelimiterValue = "").trim()
                val offset = location.substringBefore(':').toIntOrNull()
                val length = location.substringAfter(':', missingDelimiterValue = "").toIntOrNull()
                if (name.isEmpty() || offset == null || length == null || offset + length > licenseBytes.size) {
                    return@mapNotNull null
                }
                OpenSourceLicense(name, licenseBytes.decodeToString(offset, offset + length).trim())
            }.distinctBy(OpenSourceLicense::name).sortedBy { it.name.lowercase() }.toList()
        }
    }
}
