package com.example.lifelogger.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.lifelogger.data.model.Entry
import com.example.lifelogger.data.model.EntryType

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun AddEntryScreen(
    onBack: () -> Unit,
    onSave: (Entry) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var hasImage by remember { mutableStateOf(false) }
    var hasAudio by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
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
                                hasImage && hasAudio -> EntryType.MIXED
                                hasImage -> EntryType.IMAGE
                                hasAudio -> EntryType.AUDIO
                                else -> EntryType.TEXT
                            }
                            onSave(
                                Entry(
                                    title = title.text,
                                    content = content.text,
                                    entryType = entryType,
                                    imagePath = if (hasImage) "placeholder_image_path" else null,
                                    audioPath = if (hasAudio) "placeholder_audio_path" else null
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = hasImage,
                    onClick = { hasImage = !hasImage },
                    label = { Text("🖼️ Photo") },
                    leadingIcon = if (hasImage) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                FilterChip(
                    selected = hasAudio,
                    onClick = { hasAudio = !hasAudio },
                    label = { Text("🎤 Voice") },
                    leadingIcon = if (hasAudio) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }

            if (hasImage || hasAudio) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (hasImage) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🖼️ ", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                                Text("Image attached (placeholder)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (hasAudio) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎵 ", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                                Text("Audio recording (placeholder)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
