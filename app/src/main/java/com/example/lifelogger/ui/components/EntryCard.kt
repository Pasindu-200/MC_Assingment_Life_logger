package com.example.lifelogger.ui.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.model.EntryType
import com.example.lifelogger.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "EntryCard"

@Composable
fun EntryCard(
    entry: Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EntryTypeBadge(entry.entryType)
                Text(
                    text = formatDate(entry.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            if (entry.title.isNotBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Content
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Image preview
            entry.imagePath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                ImagePreview(path)
            }

            // Audio player
            entry.audioPath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                AudioPlayer(audioPath = path)
            }
        }
    }
}

@Composable
private fun AudioPlayer(audioPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }

    LaunchedEffect(audioPath) {
        try {
            val path = audioPath.removePrefix("file://")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                duration = this.duration
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio: ${e.message}")
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                currentPosition = mediaPlayer?.currentPosition ?: 0
                kotlinx.coroutines.delay(100)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlayback() {
        mediaPlayer?.let { mp ->
            if (isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
        }
    }

    fun formatTime(ms: Int): String {
        val seconds = ms / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { togglePlayback() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.Audiotrack,  // ← Now works with proper import
                contentDescription = "Audio",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EntryTypeBadge(type: EntryType) {
    val (label, color) = when (type) {
        EntryType.TEXT -> "📝 Note" to MaterialTheme.colorScheme.primary
        EntryType.IMAGE -> "🖼️ Photo" to MaterialTheme.colorScheme.secondary
        EntryType.AUDIO -> "🎤 Voice" to MaterialTheme.colorScheme.tertiary
        EntryType.MIXED -> "✨ Mixed" to MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ImagePreview(imagePath: String, modifier: Modifier = Modifier) {
    val bitmap = FileUtils.loadImage(imagePath)?.let {
        android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
    }

    if (bitmap != null) {
        Image(  // ← Now works with proper import
            bitmap = bitmap.asImageBitmap(),  // ← Now works with proper import
            contentDescription = "Entry image",
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop  // ← Now works with proper import
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
