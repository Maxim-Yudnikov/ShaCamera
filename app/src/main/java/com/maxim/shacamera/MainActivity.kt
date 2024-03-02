package com.maxim.shacamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.maxim.shacamera.databinding.ActivityMainBinding
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var previewSize: Size? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            setupCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

        }
    }

    private var cameraDevice: CameraDevice? = null
    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startPreview()
            Toast.makeText(applicationContext, "camera connected!", Toast.LENGTH_LONG).show()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    override fun onPause() {
        cameraDevice?.let {
            it.close()
            cameraDevice = null
        }

        stopBackgroundThread()

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()

        if (binding.textureView.isAvailable) {
            setupCamera(binding.textureView.width, binding.textureView.height)
            connectCamera()
        } else {
            binding.textureView.surfaceTextureListener = textureListener
        }
    }


    private var cameraId = ""
    private fun setupCamera(width: Int, height: Int) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val map =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val deviceOrientation = windowManager.defaultDisplay.orientation
                val totalRotation =
                    sensorToDeviceOrientation(cameraCharacteristics, deviceOrientation)
                val swapRotation = totalRotation == 90 || totalRotation == 270
                var rotatedWidth = width
                var rotatedHeight = height
                if (swapRotation) {
                    rotatedWidth = height
                    rotatedHeight = width
                }
                previewSize = chooseOptimalSize(
                    map!!.getOutputSizes(SurfaceTexture::class.java),
                    rotatedWidth,
                    rotatedHeight
                )
                cameraId = id
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connectCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                cameraManager.openCamera(cameraId, cameraDeviceStateCallback, handler)
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "App required access to camera", Toast.LENGTH_LONG).show()
                }
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION_RESULT
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPreview() {
        val surfaceTexture = binding.textureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        val previewSurface = Surface(surfaceTexture)
        try {
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(previewSurface)
            cameraDevice!!.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.setRepeatingRequest(captureRequestBuilder!!.build(), null, handler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(applicationContext, "Unable to setup camera preview", Toast.LENGTH_LONG).show()
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "App won't work without permission", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val bigEnough = mutableListOf<Size>()
        for (size in choices) {
            if (size.height == size.width * height / width &&
                size.width >= width && size.height >= height
            ) {
                bigEnough.add(size)
            }
        }
        return if (bigEnough.isNotEmpty()) {
            return Collections.min(bigEnough, CompareSizeByArea())
        } else {
            choices[0]
        }
    }

    private var backgroundHandlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("camera2")
        backgroundHandlerThread!!.start()
        handler = Handler(backgroundHandlerThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread!!.quitSafely()
        try {
            backgroundHandlerThread!!.join()
            backgroundHandlerThread = null
            handler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sensorToDeviceOrientation(
        cameraCharacteristics: CameraCharacteristics,
        deviceOrientation: Int
    ): Int {
        val sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        val currentDeviceOrientation = ORIENTATIONS.get(deviceOrientation)
        return (sensorOrientation!! + currentDeviceOrientation + 360) % 360
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION_RESULT = 0
        private val ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 0)
            append(Surface.ROTATION_90, 90)
            append(Surface.ROTATION_180, 180)
            append(Surface.ROTATION_270, 270)
        }
    }
}