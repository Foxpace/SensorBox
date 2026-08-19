package com.motionapps.wearoslib.files

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap

object WearFilePathCodec {
    fun encode(metadata: WearFileMetadata): AppResult<String> = if (
        metadata.measurementName.isSafePathPart() && metadata.fileName.isSafePathPart()
    ) {
        appResult(AppErrorCode.CONNECTIVITY, "Encode Wear file path") {
            "$PREFIX/${metadata.measurementName.encodePart()}/${metadata.fileName.encodePart()}"
        }
    } else {
        AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
    }

    fun decode(path: String): AppResult<WearFileMetadata> {
        val parts = path.removePrefix("$PREFIX/").split('/')
        if (!path.startsWith("$PREFIX/") || parts.size != 2) {
            return AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
        }
        return appResult(AppErrorCode.CONNECTIVITY, "Decode Wear file path") {
            WearFileMetadata(parts[0].decodePart(), parts[1].decodePart())
        }.flatMap { metadata ->
            if (metadata.measurementName.isSafePathPart() && metadata.fileName.isSafePathPart()) {
                AppResult.success(metadata)
            } else {
                AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Validate Wear file path"))
            }
        }
    }

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

    const val PREFIX = "/sensorbox/v1/file"
    private const val MAX_PART_LENGTH = 120
    private const val BYTE_MASK = 0xFF
    private const val BASE64_URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
}
