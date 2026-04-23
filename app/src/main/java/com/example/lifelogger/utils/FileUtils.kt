package com.example.lifelogger.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun getAttachmentsDir(context: Context): File {
        return File(context.filesDir, "entry_attachments").apply {
            if (!exists()) mkdirs()
        }
    }

    fun generateFilename(prefix: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_${timestamp}.${extension}"
    }

    // ← FIX: Add explicit (data: ByteArray) type
    fun saveImage(context: Context, data: ByteArray): String {
        val dir = getAttachmentsDir(context)
        val filename = generateFilename("IMG", "jpg")
        val file = File(dir, filename)
        file.writeBytes(data)
        return "file://${file.absolutePath}"
    }

    // ← FIX: Add explicit (data: ByteArray) type
    fun saveAudio(context: Context, data: ByteArray): String {
        val dir = getAttachmentsDir(context)
        val filename = generateFilename("AUD", "m4a")
        val file = File(dir, filename)
        file.writeBytes(data)
        return "file://${file.absolutePath}"
    }

    fun loadImage(path: String): ByteArray? {
        return try {
            val file = File(path.removePrefix("file://"))
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) { null }
    }

    fun loadAudio(path: String): ByteArray? {
        return try {
            val file = java.io.File(path.removePrefix("file://"))
            if (file.exists() && file.canRead()) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteAttachment(path: String): Boolean {
        return try {
            val file = File(path.removePrefix("file://"))
            if (file.exists()) file.delete() else false
        } catch (e: Exception) { false }
    }
}
