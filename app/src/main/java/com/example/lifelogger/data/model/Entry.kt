package com.example.lifelogger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// Room Entity - updated to support multiple images and deletion flag
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val entryType: EntryType,
    val tag: String? = null,
    val imagePaths: List<String> = emptyList(),
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false, // Flag for "Soft Delete" to sync with cloud
    val serverId: String? = null,
    val userId: String? = null
)

// Converters for Room to handle List<String> using kotlinx.serialization
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }
}

// Separate serializable model for Supabase
@Serializable
data class EntryDto(
    val id: String,
    val user_id: String,
    val title: String?,
    val content: String,
    val entry_type: String,
    val tag: String?,
    val image_urls: List<String>,
    val audio_url: String?,
    val created_at: String,
    val updated_at: String
)

@Serializable
enum class EntryType {
    TEXT, IMAGE, AUDIO, MIXED
}

// Conversion helpers
fun Entry.toDto(): EntryDto {
    return EntryDto(
        id = id,
        user_id = userId ?: "guest",
        title = title.takeIf { it.isNotBlank() },
        content = content,
        entry_type = entryType.name,
        tag = tag,
        image_urls = imagePaths,
        audio_url = audioPath,
        created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .format(java.util.Date(createdAt)),
        updated_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .format(java.util.Date(updatedAt))
    )
}

fun EntryDto.toEntity(userId: String? = null): Entry {
    return Entry(
        id = id,
        title = title ?: "",
        content = content,
        entryType = EntryType.valueOf(entry_type),
        tag = tag,
        imagePaths = image_urls,
        audioPath = audio_url,
        isSynced = true,
        serverId = id,
        userId = userId
    )
}
