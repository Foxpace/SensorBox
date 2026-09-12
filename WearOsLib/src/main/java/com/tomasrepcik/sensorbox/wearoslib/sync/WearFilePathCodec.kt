package com.tomasrepcik.sensorbox.wearoslib.sync

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.flatMap
import kotlinx.serialization.json.Json

object WearFilePathCodec {
    fun encode(metadata: WearFileMetadata): AppResult<String> = if (
        metadata.isValid()
    ) {
        appResult(AppErrorCode.CONNECTIVITY, "Encode Wear file path") {
            "$PREFIX/${JSON.encodeToString(metadata).encodePart()}"
        }
    } else {
        AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
    }

    fun decode(path: String): AppResult<WearFileMetadata> {
        if (!path.startsWith("$PREFIX/")) {
            return AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
        }
        return appResult(AppErrorCode.CONNECTIVITY, "Decode Wear file path") {
            val encodedMetadata = path.removePrefix("$PREFIX/")
            require(encodedMetadata.isNotBlank() && '/' !in encodedMetadata)
            JSON.decodeFromString<WearFileMetadata>(encodedMetadata.decodePart())
        }.flatMap { metadata ->
            if (metadata.isValid()) {
                AppResult.success(metadata)
            } else {
                AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
            }
        }
    }

    private fun WearFileMetadata.isValid(): Boolean =
        measurementName.isSafePathPart() && fileName.isSafePathPart() && requestId.isValidRequestId()

    private fun String.isValidRequestId(): Boolean = isNotBlank() && length <= 128 &&
        all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun String.encodePart(): String = encodeToByteArray().toBase64Url()

    private fun String.decodePart(): String = decodeBase64Url().decodeToString()

    private fun ByteArray.toBase64Url(): String = buildString((size * 4 + 2) / 3) {
        var index = 0
        while (index < size) {
            val first = this@toBase64Url[index++].toInt() and BYTE_MASK
            append(BASE64_URL_ALPHABET[first ushr 2])
            if (index < size) {
                val second = this@toBase64Url[index++].toInt() and BYTE_MASK
                append(BASE64_URL_ALPHABET[(first and 0x03) shl 4 or (second ushr 4)])
                if (index < size) {
                    val third = this@toBase64Url[index++].toInt() and BYTE_MASK
                    append(BASE64_URL_ALPHABET[(second and 0x0F) shl 2 or (third ushr 6)])
                    append(BASE64_URL_ALPHABET[third and 0x3F])
                } else {
                    append(BASE64_URL_ALPHABET[(second and 0x0F) shl 2])
                }
            } else {
                append(BASE64_URL_ALPHABET[(first and 0x03) shl 4])
            }
        }
    }

    private fun String.decodeBase64Url(): ByteArray {
        require(length % 4 != 1) { "Invalid URL-safe Base64 length" }
        val output = ByteArray(length * 3 / 4)
        var outputIndex = 0
        var buffer = 0
        var bitCount = 0
        for (character in this) {
            val value = BASE64_URL_ALPHABET.indexOf(character)
            require(value >= 0) { "Invalid URL-safe Base64 character" }
            buffer = buffer shl 6 or value
            bitCount += 6
            if (bitCount >= 8) {
                bitCount -= 8
                output[outputIndex++] = (buffer ushr bitCount).toByte()
                buffer = buffer and ((1 shl bitCount) - 1)
            }
        }
        require(buffer == 0) { "Invalid URL-safe Base64 trailing bits" }
        return output.copyOf(outputIndex)
    }

    private fun String.isSafePathPart(): Boolean =
        isNotBlank() && length <= MAX_PART_LENGTH && '/' !in this && '\\' !in this && this != "." && this != ".."

    const val PREFIX = "/sensorbox/v3/file"
    private const val MAX_PART_LENGTH = 120
    private const val BYTE_MASK = 0xFF
    private const val BASE64_URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val JSON = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }
}
