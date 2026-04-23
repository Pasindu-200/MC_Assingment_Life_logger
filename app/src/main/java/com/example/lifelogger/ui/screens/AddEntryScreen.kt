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
import com.example.lifelogger.utils.FileUtils

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

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCamera = true
        } else {
            snackbarMessage = "Camera permission required"
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showRecorder = true
        } else {
            snackbarMessage = "Microphone permission required"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = FileUtils.copyUriToInternalStorage(context, it, "IMG")
            capturedImagePath = path
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    fun requestCamera() {
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
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { requestCamera() },
                        label = { Text("📷 Camera") },
                        enabled = capturedImagePath == null && capturedAudioPath == null
                    )

                    FilterChip(
                        selected = capturedImagePath != null,
                        onClick = { galleryLauncher.launch("image/*") },
                        label = { Text("🖼️ Photo") },
                        leadingIcon = if (capturedImagePath != null) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        enabled = capturedAudioPath == null
                    )

                    FilterChip(
                        selected = capturedAudioPath != null,
                        onClick = { requestAudio() },
                        label = { Text("🎤 Voice") },
                        leadingIcon = if (capturedAudioPath != null) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        enabled = capturedImagePath == null
                    )
                }

                capturedImagePath?.let { _ ->
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
                                text = "Image attached",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { capturedImagePath = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                capturedAudioPath?.let { _ ->
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

    if (showCamera) {
        CameraCapture(
            onImageCaptured = { path ->
                capturedImagePath = path
                showCamera = false
            },
            onDismiss = {
                showCamera = false
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showRecorder) {
        AudioRecorder(
            onAudioRecorded = { path ->
                capturedAudioPath = path
                showRecorder = false
            },
            onDismiss = {
                showRecorder = false
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
