package com.maxim.shacamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var backgroundThread: HandlerThread? = null
    private var handler: Handler? = null

    private val myCameras = mutableListOf<CameraService>()
    private var cameraManager: CameraManager? = null

    private var cameraId = ""

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

    private var bitmapZoom = 1f
    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            myCameras[0].open()
            cameraId = myCameras[0].cameraId
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

    private var fingerSpacing = 0f
    private var zoomLevel = 1f
    private var maxZoomLevel = 30
    private var zoom: Rect? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

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

        binding.photoButton.setOnClickListener {
            makePhoto()
        }

        binding.changeCameraButton.setOnClickListener {
            if (myCameras[0].isOpen()) {
                myCameras[0].close()
                if (!myCameras[1].isOpen())
                    myCameras[1].open()
                cameraId = myCameras[1].cameraId
            } else if (myCameras[1].isOpen()) {
                myCameras[1].close()
                if (!myCameras[0].isOpen())
                    myCameras[0].open()
                cameraId = myCameras[0].cameraId
            }
        }

        val listener = object : View.OnTouchListener {
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

    private fun makePhoto() {
        val bitmap = binding.imageView.drawable.toBitmap()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name))
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val outputStream = contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                }
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show()
            }
        } else {
            val imageFileFolder = File(Environment.getExternalStorageDirectory().toString() + '/' + getString(R.string.app_name))
            if (!imageFileFolder.exists()) {
                imageFileFolder.mkdir()
            }
            val imageName = "${System.currentTimeMillis()}.jpg"
            val imageFile = File(imageFileFolder, imageName)
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()

        if (!binding.textureView.isAvailable)
            binding.textureView.surfaceTextureListener = textureListener
        else {
            myCameras[0].open()
            cameraId = myCameras[0].cameraId
        }

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

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundThread()
        myCameras[0].close()
        myCameras[1].close()
    }

    inner class CameraService(
        val cameraId: String,
        private val cameraManager: CameraManager
    ) {
        private var cameraDevice: CameraDevice? = null
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
        private val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "test1.jpg"
        )

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

        private fun createCameraPreviewSession() {
            imageReader = ImageReader.newInstance(binding.textureView.width, binding.textureView.height, ImageFormat.JPEG, 10)
            imageReader!!.setOnImageAvailableListener(onImageAvailableListener, null)

            val texture = binding.textureView.surfaceTexture
            val surface = Surface(texture)

            try {
                captureRequestBuilder =
                    cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder!!.addTarget(surface)
                cameraDevice!!.createCaptureSession(
                    listOf(surface, imageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSession = session
                            try {
                                cameraCaptureSession!!.setRepeatingRequest(
                                    captureRequestBuilder!!.build(),
                                    null,
                                    handler
                                )
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