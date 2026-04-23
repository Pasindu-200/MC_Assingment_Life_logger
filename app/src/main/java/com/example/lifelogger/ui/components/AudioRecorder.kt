package com.example.lifelogger.ui.components

import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow  // ← ADD THIS IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifelogger.utils.FileUtils
import kotlinx.coroutines.delay
import java.io.File  // ← ADD THIS IMPORT
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AudioRecorder(
    onAudioRecorded: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }  // ← FIX: Explicit File type

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    fun startRecording() {
        try {
            audioFile = File(  // ← FIX: Explicit File
                FileUtils.getAttachmentsDir(context),
                FileUtils.generateFilename("AUD", "m4a")
            )

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile)
                    prepare()
                    start()
                }
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile?.absolutePath)
                    prepare()
                    start()
                }
            }
            isRecording = true
        } catch (e: IOException) {
            Log.e("AudioRecorder", "Failed: ${e.message}")
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            isRecording = false

            audioFile?.let { file ->  // ← FIX: Explicit type inference
                if (file.exists() && file.length() > 0) {
                    onAudioRecorded("file://${file.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Stop failed: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaRecorder?.release(); mediaRecorder = null }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f), modifier = Modifier.matchParentSize()) {}
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error, modifier = Modifier.size(80.dp)) {}
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), modifier = Modifier.matchParentSize()) {}
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp)) {}
            }
        }

        Text(
            text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = if (isRecording) "Recording..." else "Ready to record",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { if (isRecording) stopRecording(); onDismiss() }) { Text("Cancel") }

            Button(
                onClick = { if (isRecording) stopRecording() else startRecording() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) { Text(if (isRecording) "Stop" else "Record") }

            IconButton(onClick = { /* TODO */ }, enabled = false) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}
