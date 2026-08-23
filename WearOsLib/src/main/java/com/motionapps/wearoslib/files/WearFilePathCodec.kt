package com.motionapps.wearoslib.files

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import java.util.Base64

object WearFilePathCodec {
    fun encode(metadata: WearFileMetadata): Result<String> = if (
        metadata.measurementName.isSafePathPart() && metadata.fileName.isSafePathPart()
    ) {
        appResult(AppError.Kind.CONNECTIVITY, "Encode Wear file path") {
            "$PREFIX/${metadata.measurementName.encodePart()}/${metadata.fileName.encodePart()}"
        }
    } else {
        Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear file path"))
    }

    fun decode(path: String): Result<WearFileMetadata> {
        val parts = path.removePrefix("$PREFIX/").split('/')
        if (!path.startsWith("$PREFIX/") || parts.size != 2) {
            return Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear file path"))
        }
        return appResult(AppError.Kind.CONNECTIVITY, "Decode Wear file path") {
            WearFileMetadata(parts[0].decodePart(), parts[1].decodePart())
        }.flatMap { metadata ->
            if (metadata.measurementName.isSafePathPart() && metadata.fileName.isSafePathPart()) {
                Result.success(metadata)
            } else {
                Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear file path"))
            }
        }
    }

    private fun String.encodePart(): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(encodeToByteArray())

    private fun String.decodePart(): String = Base64.getUrlDecoder().decode(this).decodeToString()

    private fun String.isSafePathPart(): Boolean =
        isNotBlank() && length <= MAX_PART_LENGTH && '/' !in this && '\\' !in this && this != "." && this != ".."

    const val PREFIX = "/sensorbox/v1/file"
    private const val MAX_PART_LENGTH = 120
}
