package com.example.lifelogger.ui.components

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam  // ← ADD THIS IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.lifelogger.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File  // ← ADD THIS IMPORT
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log

private const val TAG = "CameraCapture"

@Composable
fun CameraCapture(
    onImageCaptured: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(previewView) {
        Log.d(TAG, "Initializing camera, previewView: $previewView")
        previewView?.let { view ->
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "CameraProvider obtained")
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraCapture", "Init failed: ${e.message}")
            }?: Log.w(TAG, "previewView is null")
        }
    }

    fun takePhoto() {
        if (imageCapture == null || isTakingPhoto) return
        isTakingPhoto = true
        val photoFile = File(  // ← FIX: Explicit File
            FileUtils.getAttachmentsDir(context),
            FileUtils.generateFilename("IMG", "jpg")
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()  // ← FIX: .build()

        imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isTakingPhoto = false
                    onImageCaptured("file://${photoFile.absolutePath}")
                }
                override fun onError(exception: ImageCaptureException) {
                    isTakingPhoto = false
                    Log.e("CameraCapture", "Capture failed: ${exception.message}")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
            update = { previewView = it },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }

            FloatingActionButton(
                onClick = {
                    if (!isTakingPhoto) takePhoto()  // ← Check state inside onClick
                },
                containerColor = if (isTakingPhoto)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)  // ← Visual disabled state
                else
                    MaterialTheme.colorScheme.primary
            ) {
                if (isTakingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("📷", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
            }

            IconButton(onClick = { /* TODO */ }, enabled = false) {
                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}
