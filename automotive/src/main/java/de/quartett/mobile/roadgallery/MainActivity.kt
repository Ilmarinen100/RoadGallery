package de.quartett.mobile.roadgallery

import android.car.Car
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpackcomposeexample.ui.theme.RoadGalleryTheme
import global.covesa.sdk.client.push.ActionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.hardware.SensorManager

class MainActivity : ComponentActivity() {
    private val permissions =
        arrayOf(Manifest.permission.CAMERA, Car.PERMISSION_SPEED, Car.PERMISSION_POWERTRAIN)

    private var viewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContent {
            RoadGalleryTheme {
                val viewModel: MainViewModel = viewModel {

                    MainViewModel(
                        context = this@MainActivity,
                        pushUiState = PushUiState(this@MainActivity),
                    ).also { viewModel = it }
                }
                val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
                val cameraViewModel = viewModel {
                    CameraViewModel(sensorManager, this@MainActivity)
                }
                MainView(viewModel, cameraViewModel)
            }
        }
        subscribeActions()
    }

    private var job: Job? = null
    private fun subscribeActions() {
        job = CoroutineScope(Dispatchers.IO).launch {
            ActionEvent.events.collect {
                it.handleAction(this@MainActivity)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            viewModel?.connect()
        } else {
            try {
                requestPermissions(permissions, 0)
            } catch (e: Throwable) {
                println("failure $e")
            }

        }

        viewModel?.connect()
    }

    override fun onPause() {
        viewModel?.disconnect()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if (permissions.indexOf(Car.PERMISSION_SPEED) != -1
            && grantResults[permissions.indexOf(Car.PERMISSION_SPEED)] == PackageManager.PERMISSION_GRANTED
            && permissions.indexOf(Car.PERMISSION_POWERTRAIN) != -1
            && grantResults[permissions.indexOf(Car.PERMISSION_POWERTRAIN)] == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel?.connect()
        }
    }
}

