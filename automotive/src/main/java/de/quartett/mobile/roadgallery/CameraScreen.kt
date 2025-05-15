package de.quartett.mobile.roadgallery

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    Column {
        var foo by remember { mutableIntStateOf(0) }

        val uiState by viewModel.state.collectAsStateWithLifecycle()

        val accel: Boolean by viewModel.deboucedAccel.collectAsStateWithLifecycle()

        Button(onClick = {
            Log.i("UI", "click")
            viewModel.changeLens()
            foo += 1

        }, content = { Text("Change Cameras") })

        LaunchedEffect(accel) {
            if (accel) {
                Log.i("accel2", "RISING EDGE")
                recordImage(viewModel, viewModel.context)
            }
        }
        if (accel) {
            Text("acceleration detected", color = Color.Red)
        } else {
            Text("at rest", color = Color.Green)
        }


        val context = LocalContext.current

        val lifecycleOwner = LocalLifecycleOwner.current


        val cameraIDs = getCameraIds(context)
        Log.i("cameraIds", cameraIDs.joinToString())


        if (cameraIDs.isEmpty()) {
            Log.e("Camera", "No Camera")
        }
        val preview = Preview.Builder().build()
        val previewView = remember {
            PreviewView(context)
        }

        key(foo) {
            Text("$foo")

            Log.i("recompose", "happened")
            LaunchedEffect(viewModel.state) {
                Log.i("recompose", "launched")
                val cameraProvider = getCameraProvider(context)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, uiState, preview)
                preview.surfaceProvider = previewView.surfaceProvider
            }
        }
        AndroidView(
            factory = { previewView }, modifier = Modifier
                .width(300.dp)
                .height(300.dp)
        )
        ShowImage(viewModel)
    }
}


suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }

fun getCameraIds(context: Context): List<String> {
    val cameraIds = mutableListOf<String>()

    // Get the CameraManager system service
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    try {
        // Get a list of all camera IDs
        cameraIds.addAll(cameraManager.cameraIdList)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return cameraIds
}


@Composable
fun ShowImage(viewModel: CameraViewModel) {
    val imageData by viewModel.imageFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (imageData != null) {
            val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(imageData))
            val imageBitmap: ImageBitmap = bitmap.asImageBitmap()

            Image(
                bitmap = imageBitmap,
                contentDescription = "Captured Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "No image captured yet!",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


suspend fun recordImage(viewModel: CameraViewModel, context: Context) {
    val lifecycleOwner = context as? LifecycleOwner
        ?: throw IllegalStateException("Context is not a LifecycleOwner")
    val imageCapture = ImageCapture.Builder().build()
    val cameraProvider = getCameraProvider(context)
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(lifecycleOwner, viewModel.state.value, imageCapture)
    val outputStream = ByteArrayOutputStream()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("CameraX", "Image saved to: ${outputFileResults.savedUri}")
                val imageData = outputStream.toByteArray()
                viewModel.updateImageData(imageData)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
            }
        }
    )
}