package com.example.lifelogger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.model.EntryType
import com.example.lifelogger.ui.components.AudioRecorder
import com.example.lifelogger.ui.components.CameraCapture

private const val TAG = "AddEntryScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onBack: () -> Unit,
    onSave: (Entry) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var capturedAudioPath by remember { mutableStateOf<String?>(null) }

    var showCamera by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }

    // ← FIX: Snackbar state managed via Scaffold, not called in callbacks
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission launchers - NO composables inside callbacks
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Camera permission: $granted")
        if (granted) {
            showCamera = true
        } else {
            // ← FIX: Set message, don't call Snackbar composable here
            snackbarMessage = "Camera permission required"
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Audio permission: $granted")
        if (granted) {
            showRecorder = true
        } else {
            // ← FIX: Set message, don't call Snackbar composable here
            snackbarMessage = "Microphone permission required"
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null  // Clear after showing
        }
    }

    fun requestCamera() {
        Log.d(TAG, "Requesting camera")
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                showCamera = true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun requestAudio() {
        Log.d(TAG, "Requesting audio")
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> {
                showRecorder = true
            }
            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Main content
    if (!showCamera && !showRecorder) {
        Scaffold(  // ← FIX: Add snackbarHost parameter
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },  // ← FIX: Proper Snackbar host
            topBar = {
                TopAppBar(
                    title = { Text("New Entry") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val entryType = when {
                                    capturedImagePath != null && capturedAudioPath != null -> EntryType.MIXED
                                    capturedImagePath != null -> EntryType.IMAGE
                                    capturedAudioPath != null -> EntryType.AUDIO
                                    else -> EntryType.TEXT
                                }
                                onSave(
                                    Entry(
                                        title = title.text,
                                        content = content.text,
                                        entryType = entryType,
                                        imagePath = capturedImagePath,
                                        audioPath = capturedAudioPath
                                    )
                                )
                            },
                            enabled = content.text.isNotBlank() || title.text.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    minLines = 4
                )

                Text(text = "Add attachments", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = capturedImagePath != null,
                        onClick = {
                            Log.d(TAG, "Camera chip clicked")
                            requestCamera()
                        },
                        label = { Text("📷 Photo") },
                        leadingIcon = if (capturedImagePath != null) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        enabled = capturedAudioPath == null
                    )

                    FilterChip(
                        selected = capturedAudioPath != null,
                        onClick = {
                            Log.d(TAG, "Audio chip clicked")
                            requestAudio()
                        },
                        label = { Text("🎤 Voice") },
                        leadingIcon = if (capturedAudioPath != null) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        enabled = capturedImagePath == null
                    )
                }

                capturedImagePath?.let { path ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🖼️ ", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                            Text(
                                text = "Image captured",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { capturedImagePath = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                capturedAudioPath?.let { path ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎵 ", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                            Text(
                                text = "Audio recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { capturedAudioPath = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Camera/Audio overlays - outside Scaffold, still in @Composable function ✅
    if (showCamera) {
        Log.d(TAG, "Showing CameraCapture")
        CameraCapture(
            onImageCaptured = { path ->
                Log.d(TAG, "Image captured: $path")
                capturedImagePath = path
                showCamera = false
            },
            onDismiss = {
                Log.d(TAG, "Camera dismissed")
                showCamera = false
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showRecorder) {
        Log.d(TAG, "Showing AudioRecorder")
        AudioRecorder(
            onAudioRecorded = { path ->
                Log.d(TAG, "Audio recorded: $path")
                capturedAudioPath = path
                showRecorder = false
            },
            onDismiss = {
                Log.d(TAG, "Recorder dismissed")
                showRecorder = false
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
