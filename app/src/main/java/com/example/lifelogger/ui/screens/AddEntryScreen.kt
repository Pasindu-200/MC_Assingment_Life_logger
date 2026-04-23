package com.example.lifelogger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEntryScreen(
    onBack: () -> Unit,
    onSave: (Entry) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    // Support multiple images
    var capturedImagePaths by remember { mutableStateOf(listOf<String>()) }
    var capturedAudioPath by remember { mutableStateOf<String?>(null) }

    var showCamera by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }

    val tags = listOf(
        "Love" to Color.Red,
        "Freedom" to Color.Yellow,
        "Stress" to Color(0xFFFFA500), // Orange
        "Happy" to Color.Green,
        "Calm" to Color(0xFFADD8E6), // Light Blue
        "Sad" to Color.Black
    )

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
            path?.let { p ->
                capturedImagePaths = capturedImagePaths + p
            }
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
                                    capturedImagePaths.isNotEmpty() && capturedAudioPath != null -> EntryType.MIXED
                                    capturedImagePaths.isNotEmpty() -> EntryType.IMAGE
                                    capturedAudioPath != null -> EntryType.AUDIO
                                    else -> EntryType.TEXT
                                }
                                onSave(
                                    Entry(
                                        title = title.text,
                                        content = content.text,
                                        entryType = entryType,
                                        tag = selectedTag,
                                        imagePaths = capturedImagePaths,
                                        audioPath = capturedAudioPath
                                    )
                                )
                            },
                            enabled = content.text.isNotBlank() || title.text.isNotBlank() || capturedImagePaths.isNotEmpty() || capturedAudioPath != null
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

                Text(text = "Select Tag", style = MaterialTheme.typography.titleMedium)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { (tagName, tagColor) ->
                        val isSelected = selectedTag == tagName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTag = if (isSelected) null else tagName },
                            label = { Text(tagName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = if (isSelected) Color.White else tagColor,
                                selectedLabelColor = if (tagColor == Color.Yellow || tagColor == Color(0xFFADD8E6)) Color.Black else Color.White,
                                selectedContainerColor = tagColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = tagColor,
                                selectedBorderColor = tagColor
                            )
                        )
                    }
                }

                Text(text = "Add attachments", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { requestCamera() }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                    }

                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    }

                    IconButton(onClick = { requestAudio() }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                }

                // Multiple Image Preview
                if (capturedImagePaths.isNotEmpty()) {
                    Text(text = "Attached Images (${capturedImagePaths.size})", style = MaterialTheme.typography.labelMedium)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(capturedImagePaths) { path ->
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                                val bitmap = FileUtils.loadImage(path)?.let {
                                    android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                IconButton(
                                    onClick = { capturedImagePaths = capturedImagePaths - path },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
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
                capturedImagePaths = capturedImagePaths + path
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
