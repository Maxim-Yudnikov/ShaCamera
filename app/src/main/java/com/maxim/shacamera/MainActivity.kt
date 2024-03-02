package com.maxim.shacamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.maxim.shacamera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var previewSize: Size? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var isRecording = false

    private var videoFolder: File? = null
    private var videoFileName = ""

    private var bitmapZoom = 1f

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            setupCamera(width, height)
            setupCamera(binding.textureView.width, binding.textureView.height)
            connectCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            val bitmap = binding.textureView.bitmap!!
            val width = (binding.textureView.width / bitmapZoom).toInt()
            val height = (binding.textureView.height / bitmapZoom).toInt()
            val newBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - width / 2,
                bitmap.height / 2 - height / 2,
                width,
                height
            )
            binding.imageView.setImageBitmap(newBitmap)
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

    private var fingerSpacing = 0f
    private var zoomLevel = 1f
    private var maxZoomLevel = 30
    private var zoom: Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createVideoFolder()

        binding.recordingButton.setOnClickListener {
            if (isRecording) {
                isRecording = false
                binding.recordingButton.setImageResource(R.drawable.recording_24)
            }
            if (!isRecording) {
                checkWriteStoragePermission()
            }
        }

        val listener = object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val rect =
                    cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                        ?: return false

                if (event!!.pointerCount == 2) {
                    val currentFingerSpacing = getFingerSpacing(event)
                    var delta = 0.8f //Control this value to control the zooming sensibility
                    if (fingerSpacing != 0f) {
                        if (currentFingerSpacing > fingerSpacing) {
                            if ((maxZoomLevel - zoomLevel) <= delta) {
                                delta = maxZoomLevel - zoomLevel
                                if ((binding.textureView.width / (bitmapZoom + bitmapZoom / 25)).toInt() != 0)
                                    bitmapZoom += bitmapZoom / 25 //Control this value to control the bitmap zooming sensibility
                                Log.d("MyLog", "bitmap zoom: $bitmapZoom")
                            }
                            zoomLevel += delta
                        } else if (currentFingerSpacing < fingerSpacing) {
                            if ((zoomLevel - delta) < 1f) {
                                delta = zoomLevel - 1f
                            }
                            if (bitmapZoom == 1f)
                                zoomLevel -= delta
                            else {
                                bitmapZoom -= bitmapZoom / 25 //Control this value to control the bitmap zooming sensibility
                                if (bitmapZoom < 1f)
                                    bitmapZoom = 1f
                                Log.d("MyLog", "bitmap zoom: $bitmapZoom")
                            }
                        }

                        val ratio = 1f / zoomLevel
                        val croppedWidth =
                            rect.width() - (rect.width().toFloat() * ratio).roundToInt()
                        val croppedHeight =
                            rect.height() - (rect.height().toFloat() * ratio).roundToInt()
                        zoom = Rect(
                            croppedWidth / 2, croppedHeight / 2,
                            rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2
                        )
                        //Log.d("MyLog", "w: ${zoom!!.width()}, h: ${zoom!!.height()}")
                        captureRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
                        val zoomValueText =
                            if (bitmapZoom == 1f) "${(zoomLevel * 10).roundToInt() / 10f}x" else
                                "${(zoomLevel * bitmapZoom * 10).roundToInt() / 10}x ($maxZoomLevel*${(bitmapZoom * 10).roundToInt() / 10f})"
                        binding.zoomValueTextView.text = zoomValueText
                    }
                    fingerSpacing = currentFingerSpacing
                } else {
                    return true
                }
                cameraCaptureSession!!.setRepeatingRequest(
                    captureRequestBuilder!!.build(),
                    null,
                    handler
                )
                return true
            }
        }
        binding.textureView.setOnTouchListener(listener)
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

    //todo
    private var cameraCaptureSession: CameraCaptureSession? = null
    private fun startPreview() {
        val surfaceTexture = binding.textureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        val previewSurface = Surface(surfaceTexture)
        try {
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(previewSurface)
            cameraDevice!!.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            cameraCaptureSession = session
                            session.setRepeatingRequest(
                                captureRequestBuilder!!.build(),
                                null,
                                handler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(
                            applicationContext,
                            "Unable to setup camera preview",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                null
            )
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
            if (grantResults.first() != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "App won't work without permission", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if (grantResults.first() != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "App won't work without permission", Toast.LENGTH_LONG).show()
                finish()
            } else {
                isRecording = true
                binding.recordingButton.setImageResource(R.drawable.recording_online_24)
                try {
                    createVideoFileName()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

    private fun createVideoFolder() {
        val movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        videoFolder = File(movieFile, "camera2-test")
        if (!videoFolder!!.exists()) {
            videoFolder!!.mkdir()
        }
    }

    private fun createVideoFileName(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val prepend = "VIDEO_${timestamp}_"
        val videoFile = File.createTempFile(prepend, ".mp4", videoFolder)
        videoFileName = videoFile.absolutePath
        return videoFile
    }

    private fun checkWriteStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            isRecording = true
            binding.recordingButton.setImageResource(R.drawable.recording_online_24)
            try {
                createVideoFileName()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "App needs to be able to save content", Toast.LENGTH_LONG)
                    .show()
            }
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT
            )
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION_RESULT = 0
        private const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1
        private val ORIENTATIONS = SparseIntArray().apply {
            append(Surface.ROTATION_0, 0)
            append(Surface.ROTATION_90, 90)
            append(Surface.ROTATION_180, 180)
            append(Surface.ROTATION_270, 270)
        }
    }

//    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
//        val rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
//            ?: return false
//
//        if (event!!.pointerCount == 2) {
//            val currentFingerSpacing = getFingerSpacing(event)
//            var delta = 0.05f //Control this value to control the zooming sensibility
//            if (fingerSpacing != 0f) {
//                if (currentFingerSpacing > fingerSpacing) {
//                    if ((maxZoomLevel - fingerSpacing) <= delta) {
//                        delta = maxZoomLevel - zoomLevel
//                    }
//                    zoomLevel += delta
//                } else if (currentFingerSpacing < fingerSpacing) {
//                    if ((zoomLevel - delta) < 1f) {
//                        delta = zoomLevel - 1f
//                    }
//                    zoomLevel -= delta
//                }
//
//                val ratio = 1f / zoomLevel
//                val croppedWidth = rect.width() - (rect.width().toFloat() * ratio).roundToInt()
//                val croppedHeight = rect.height() - (rect.height().toFloat() * ratio).roundToInt()
//                zoom = Rect(croppedWidth/2, croppedHeight/2,
//                    rect.width() - croppedWidth/2, rect.height() - croppedHeight/2)
//                Log.d("MyLog", "2")
//                captureRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
//            }
//            fingerSpacing = currentFingerSpacing
//        } else {
//            return true
//        }
//        return true
//    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }
}