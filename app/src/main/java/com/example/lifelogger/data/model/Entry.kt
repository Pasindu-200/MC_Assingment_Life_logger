package com.example.lifelogger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

// Room Entity - keep Room annotations separate from serialization
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val entryType: EntryType,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val serverId: String? = null,
    val userId: String? = null
)

// Separate serializable model for Supabase (avoids kapt conflicts)
@Serializable
data class EntryDto(
    val id: String,
    val user_id: String,
    val title: String?,
    val content: String,
    val entry_type: String,
    val image_url: String?,
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
        image_url = imagePath,
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
        imagePath = image_url,
        audioPath = audio_url,
        isSynced = true,
        serverId = id,
        userId = userId
    )
}
