package de.quartett.mobile.roadgallery

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    var foo by remember { mutableIntStateOf(0) }

    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val accel: Boolean by viewModel.deboucedAccel.collectAsStateWithLifecycle()

    Button(onClick = {
        Log.i("UI", "click")
        viewModel.changeLens(foo.absoluteValue)
        foo += 1

    }, content = {Text("Change Cameras")})

    LaunchedEffect(accel) {
        if (accel) {
            Log.i("accel2", "RISING EDGE")
        }
    }
        if (accel) {
            Text("acceleration detected", color=Color.Red)
        } else {
            Text("at rest", color= Color.Green)
        }


    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current


    val cameraIDs = getCameraIds(context)
    Log.i("cameraIds", cameraIDs.joinToString())


    if (cameraIDs.isEmpty()){
        Log.e("Camera", "No Camera")
    }
    val preview = Preview.Builder().build()
    val previewView = remember {
        PreviewView(context)
    }

    key ( foo ) {
        Text("$foo ${uiState.first}")

        Log.i("recompose", "happened")
        LaunchedEffect(viewModel.state) {
            Log.i("recompose", "launched")
            val cameraProvider = getCameraProvider(context)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, uiState.second, preview)
            preview.surfaceProvider = previewView.surfaceProvider
        }

    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
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