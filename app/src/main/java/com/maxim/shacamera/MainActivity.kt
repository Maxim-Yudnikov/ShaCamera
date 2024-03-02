package com.maxim.shacamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.maxim.shacamera.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var backgroundThread: HandlerThread? = null
    private var handler: Handler? = null

    private val myCameras = mutableListOf<CameraService>()
    private var cameraManager: CameraManager? = null

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("camera-background")
        backgroundThread!!.start()
        handler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            handler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            myCameras.clear()
            myCameras.addAll(cameraManager!!.cameraIdList.map {
                CameraService(it, cameraManager!!)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.actionButton.setOnClickListener {
            if (myCameras[0].isOpen()) myCameras[0].makePhoto()
            else if (myCameras[1].isOpen()) myCameras[1].makePhoto()
        }

        binding.changeCameraButton.setOnClickListener {
            if (myCameras[0].isOpen()) {
                myCameras[0].close()
                if (!myCameras[1].isOpen())
                    myCameras[1].open()
            } else if (myCameras[1].isOpen()) {
                myCameras[1].close()
                if (!myCameras[0].isOpen())
                    myCameras[0].open()
            }
        }

        myCameras[0].open()
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()

        val permissionList = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionList.isNotEmpty()) {
            requestPermissions(
                permissionList.toTypedArray(), 1
            )
        }
    }

    override fun onPause() {
        super.onPause()
        //stopBackgroundThread()
    }

    inner class CameraService(
        private val cameraId: String,
        private val cameraManager: CameraManager
    ) {
        private var cameraDevice: CameraDevice? = null
        private var captureSession: CameraCaptureSession? = null
        private var imageReader: ImageReader? = null
        private val cameraCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.d("MyLog", "error: $error, cameraId: ${camera.id}")
            }
        }
        private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
            handler!!.post(ImageServer(reader.acquireLatestImage(), file))
        }
        private val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "test1.jpg")

        @SuppressLint("MissingPermission")
        fun open() {
            try {
                cameraManager.openCamera(cameraId, cameraCallback, handler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        fun close() {
            cameraDevice?.let {
                it.close()
                cameraDevice = null
            }
        }

        fun isOpen() = cameraDevice != null

        fun makePhoto() {
            try {
                val captureBuilder =
                    cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(imageReader!!.surface)
                val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                    }
                }
                captureSession!!.stopRepeating()
                captureSession!!.abortCaptures()
                captureSession!!.capture(captureBuilder.build(), captureCallback, handler)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun createCameraPreviewSession() {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 10)
            imageReader!!.setOnImageAvailableListener(onImageAvailableListener, null)

            val texture = binding.textureView.surfaceTexture
            val surface = Surface(texture)

            try {
                val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                builder.addTarget(surface)
                cameraDevice!!.createCaptureSession(
                    listOf(surface, imageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                captureSession!!.setRepeatingRequest(builder.build(), null, handler)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                    },
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}