package com.maxim.shacamera.camera.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import com.easystudio.rotateimageview.RotateZoomImageView
import com.maxim.shacamera.R
import com.maxim.shacamera.camera.data.ScreenSizeMode
import com.maxim.shacamera.core.presentation.BaseFragment
import com.maxim.shacamera.databinding.FragmentCameraBinding
import com.maxim.shacamera.main.CheckPermission
import com.maxim.shacamera.stickers.data.ShowSticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>(), ManageCamera,
    ShowSticker {
    override val viewModelClass = CameraViewModel::class.java
    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCameraBinding.inflate(inflater, container, false)

    private var backgroundThread: HandlerThread? = null
    private var handler: Handler? = null

    private var cameraManager: CameraManager? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            val size = setupPreviewSize(viewModel.currentCamera(), isDimensionSwapped())
            binding.cameraTextureView.surfaceTexture!!.setDefaultBufferSize(size.width, size.height)
            viewModel.openCamera()
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            val bitmap = binding.cameraTextureView.bitmap!!
            val width = (binding.cameraTextureView.width / viewModel.bitmapZoom()).toInt()
            val height = (binding.cameraTextureView.height / viewModel.bitmapZoom()).toInt()
            val scaledBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - width / 2,
                bitmap.height / 2 - height / 2,
                width,
                height
            )

            if (!isRecording) {
                viewModel.cameraFilters().forEach {
                    it.showFilter(scaledBitmap)
                }
                viewModel.cameraFilters().forEach {
                    it.showImage(scaledBitmap, requireContext(), viewModel.bitmapZoom())
                }
            }

            binding.imageView.setImageBitmap(scaledBitmap)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private val zoomListener = View.OnTouchListener { _, event ->
        if (!viewModel.currentCamera().isOpen())
            false
        else
            viewModel.handleZoom(
                cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId()),
                event!!,
                listOf(binding.cameraTextureView.width, binding.cameraTextureView.height).min(),
                captureRequestBuilder!!,
                cameraCaptureSession!!,
                handler!!,
                isRecording
            )
    }

    private var isRecording = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        viewModel.setCameras(cameraManager!!.cameraIdList.map { id ->
            CameraService.Base(id, cameraManager!!, this)
        })

        var isRecordingOnDown = false
        binding.photoButton.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                isRecordingOnDown = isRecording
                if (isRecording) {
                    stopRecording()
                }
            }
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (!isRecordingOnDown && !isRecording)
                    viewModel.makePhoto()
            }
            false
        }

        binding.photoButton.setOnLongClickListener {
            if (isRecording) return@setOnLongClickListener false

            startRecording()

            false
        }

        binding.changeCameraButton.setOnClickListener {
            if (!isRecording)
                viewModel.changeCamera()
        }

        binding.settingsButton.setOnClickListener {
            if (!isRecording)
                viewModel.settings()
        }

        binding.stickersButton.setOnClickListener {
            if (!isRecording)
                viewModel.stickers()
        }

        binding.imageView.setOnTouchListener(zoomListener)

        viewModel.observe(this) {
            it.show(binding.zoomValueTextView)
        }

        viewModel.init(savedInstanceState == null, this)
    }

    override fun onResume() {
        if (!(requireActivity() as CheckPermission).checkPermissions()) {
            Toast.makeText(requireContext(), "Please grant all permissions", Toast.LENGTH_LONG)
                .show()
            requireActivity().finish()
        }

        super.onResume()
        viewModel.onResume()

        if (!binding.cameraTextureView.isAvailable) {
            binding.cameraTextureView.surfaceTextureListener = textureListener
        } else {
            viewModel.openCamera()
        }
    }

    override fun onPause() {
        if (isRecording)
            stopRecording()

        super.onPause()
        viewModel.onPause()
    }

    override fun openCamera(camera: CameraService) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            camera.openCamera(handler!!)
        }
    }

    override fun startRecording() {
        isRecording = true

        binding.stickersLayout.children.forEach {
            it.visibility = View.GONE
        }
        viewModel.resetBitmapZoom(
            cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId()),
            captureRequestBuilder!!
        )

        setupMediaRecorder()

        viewModel.currentCamera().createVideoCameraPreviewSession()

        mediaRecorder!!.start()
        Toast.makeText(requireContext(), "Start recording", Toast.LENGTH_SHORT).show()
        binding.photoButton.setBackgroundResource(R.drawable.recording)
        @Suppress("DEPRECATION") val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        else
            vibrator.vibrate(100)
    }

    override fun stopRecording() {
        isRecording = false
        try {
            cameraCaptureSession!!.stopRepeating()
            cameraCaptureSession!!.abortCaptures()
            cameraCaptureSession!!.close()
        } catch (_: Exception) {}
        mediaRecorder!!.stop()
        mediaRecorder!!.release()
        viewModel.currentCamera().createCameraPreviewSession()
        Toast.makeText(requireContext(), "Video saved", Toast.LENGTH_SHORT).show()
        binding.photoButton.setBackgroundResource(R.drawable.take_photo_24)
        binding.stickersLayout.children.forEach {
            it.visibility = View.VISIBLE
        }
    }

    override fun makePhoto() {
        val mainBitmap = binding.imageView.drawable.toBitmap()
        binding.stickersLayout.draw(Canvas(mainBitmap))

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        }

        val imageFileFolder = File(
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera"
        )
        if (!imageFileFolder.exists()) {
            imageFileFolder.mkdir()
        }
        val imageName = "SHACAMERA_IMG_${
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())
        }.jpg"
        val imageFile = File(imageFileFolder, imageName)
        val outputStream = FileOutputStream(imageFile)
        mainBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()
        values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)

        CoroutineScope(Dispatchers.Main + Job()).launch(Dispatchers.Main) {
            binding.flash.visibility = View.VISIBLE
            delay(100)
            binding.flash.visibility = View.GONE
        }
    }

    override fun startBackgroundThread() {
        backgroundThread = HandlerThread("camera-background")
        backgroundThread!!.start()
        handler = Handler(backgroundThread!!.looper)
    }

    override fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        backgroundThread!!.join()
        backgroundThread = null
        handler = null
    }

    override fun createCameraPreviewSession(cameraDevice: CameraDevice) {
        val texture = binding.cameraTextureView.surfaceTexture
        val surface = Surface(texture)

        val isDimensionSwapped = isDimensionSwapped()
        val size = setupPreviewSize(viewModel.currentCamera(), isDimensionSwapped)
        texture!!.setDefaultBufferSize(size.width, size.height)
        updateAspectRatio(
            viewModel.screenSizeMode(),
            size,
            isDimensionSwapped
        )

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder!!.addTarget(surface)
        viewModel.setZoom(captureRequestBuilder!!)
        cameraDevice.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        session.setRepeatingRequest(
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
    }

    override fun createVideoCameraPreviewSession(cameraDevice: CameraDevice) {
        val texture = binding.cameraTextureView.surfaceTexture
        val surface = Surface(texture)

        val isDimensionSwapped = isDimensionSwapped()
        val size = setupPreviewSize(viewModel.currentCamera(), isDimensionSwapped)
        texture!!.setDefaultBufferSize(size.width, size.height)
        updateAspectRatio(
            viewModel.screenSizeMode(),
            size,
            isDimensionSwapped
        )

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder!!.addTarget(surface)
        captureRequestBuilder!!.addTarget(mediaRecorder!!.surface)
        cameraDevice.createCaptureSession(
            listOf(surface, mediaRecorder!!.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    try {
                        session.setRepeatingRequest(
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
    }

    private fun updateAspectRatio(
        mode: ScreenSizeMode,
        previewSize: Size,
        isDimensionSwapped: Boolean
    ) {
        val displaySize = Point()
        ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)

        mode.setAspectRatio(binding.cameraTextureView, displaySize, previewSize, isDimensionSwapped)
        Handler(Looper.getMainLooper()).post {
            binding.imageView.layoutParams = binding.cameraTextureView.layoutParams
            binding.stickersLayout.layoutParams = binding.cameraTextureView.layoutParams
        }
    }

    private fun setupPreviewSize(camera: CameraService, isDimensionSwapped: Boolean): Size {
        val largest = camera.getCaptureSize(viewModel.screenSizeMode().comparator())
        val displaySize = Point()
        ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)

        return if (isDimensionSwapped)
            camera.getOptimalPreviewSize(
                binding.cameraTextureView.height,
                binding.cameraTextureView.width,
                displaySize.y,
                displaySize.x,
                largest,
                viewModel.dlssMode() == 2
            ) else
            camera.getOptimalPreviewSize(
                binding.cameraTextureView.width,
                binding.cameraTextureView.height,
                displaySize.x,
                displaySize.y,
                largest,
                viewModel.dlssMode() == 2
            )
    }

    private fun isDimensionSwapped(): Boolean {
        val displayRotation = ContextCompat.getDisplayOrDefault(requireActivity()).rotation
        val sensorOrientation =
            cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId())
                .get(CameraCharacteristics.SENSOR_ORIENTATION)
        return when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                sensorOrientation == 90 || sensorOrientation == 270
            }

            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                sensorOrientation == 0 || sensorOrientation == 180
            }

            else -> false
        }
    }

    override fun setupMediaRecorder() {
        mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else MediaRecorder()

        val videoFolder =
            File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/Camera")
        if (!videoFolder.exists()) {
            videoFolder.mkdir()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoName = "SHACAMERA_VIDEO_$timestamp.mp4"

        mediaRecorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile("$videoFolder/$videoName")
            setVideoFrameRate(30)
            setVideoSize(
                1280,
                720
            )
            val isFrontCamera =
                cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId())
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            setOrientationHint(
                if (isFrontCamera) 270 else 90
            )
            setVideoEncodingBitRate(10_000_000)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(200_000)
            setAudioSamplingRate(48_000_000)
        }
        mediaRecorder!!.prepare()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun showSticker(drawableId: Int) {
        val imageView = RotateZoomImageView(requireContext()).apply {
            layoutParams = RelativeLayout.LayoutParams(400, 400)
            setImageResource(drawableId)
        }
        var doubleClick = false
        imageView.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (doubleClick)
                    binding.stickersLayout.removeView(v)
                doubleClick = true
                Handler().postDelayed({ doubleClick = false }, 400)
            }

            imageView.onTouch(v, event)
        }
        binding.stickersLayout.addView(imageView)
    }
}

interface ManageCamera {
    fun openCamera(camera: CameraService)
    fun startRecording()
    fun setupMediaRecorder()
    fun stopRecording()
    fun makePhoto()
    fun startBackgroundThread()
    fun stopBackgroundThread()
    fun createCameraPreviewSession(cameraDevice: CameraDevice)
    fun createVideoCameraPreviewSession(cameraDevice: CameraDevice)
}