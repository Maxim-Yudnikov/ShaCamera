package com.maxim.shacamera.camera.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.R
import com.maxim.shacamera.camera.data.ScreenSizeMode
import com.maxim.shacamera.core.presentation.BaseFragment
import com.maxim.shacamera.databinding.FragmentCameraBinding
import com.maxim.shacamera.stickers.data.ShowSticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>(), ManageCamera, ShowSticker {
    override val viewModelClass = CameraViewModel::class.java
    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCameraBinding.inflate(inflater, container, false)

    private var backgroundThread: HandlerThread? = null
    private var handler: Handler? = null

    private var cameraManager: CameraManager? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            viewModel.openCamera()
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            val bitmap = binding.textureView.bitmap!!
            val width = (binding.textureView.width / viewModel.bitmapZoom()).toInt()
            val height = (binding.textureView.height / viewModel.bitmapZoom()).toInt()
            val scaledBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - width / 2,
                bitmap.height / 2 - height / 2,
                width,
                height
            )

            viewModel.cameraFilters().forEach {
                it.showFilter(scaledBitmap)
            }
            viewModel.cameraFilters().forEach {
                it.showImage(scaledBitmap, requireContext(), viewModel.bitmapZoom())
            }

            binding.imageView.setImageBitmap(scaledBitmap)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false
    }

    //todo
    @SuppressLint("ClickableViewAccessibility")
    private val zoomListener = View.OnTouchListener { _, event ->
        viewModel.handleZoom(
            cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId().toString()),
            event!!,
            listOf(binding.textureView.width, binding.textureView.height).min(),
            binding.zoomValueTextView,
            captureRequestBuilder!!,
            cameraCaptureSession!!,
            handler!!
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        viewModel.setCameras(cameraManager!!.cameraIdList.map { id ->
            CameraService.Base(id, cameraManager!!, this)
        })

        binding.photoButton.setOnClickListener {
            viewModel.makePhoto()
        }

        binding.changeCameraButton.setOnClickListener {
            viewModel.changeCamera()
        }

        binding.settingsButton.setOnClickListener {
            viewModel.settings()
        }

        binding.stickersButton.setOnClickListener {
            viewModel.stickers()
        }

        binding.imageView.setOnTouchListener(zoomListener)

        viewModel.init(savedInstanceState == null, this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()

        if (!binding.textureView.isAvailable)
            binding.textureView.surfaceTextureListener = textureListener
        else {
            viewModel.openCamera()
        }
    }

    override fun onPause() {
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

    override fun makePhoto() {
        val mainBitmap = binding.imageView.drawable.toBitmap()
        binding.stickersLayout.draw(Canvas(mainBitmap))

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        }
        val contentResolver = requireContext().contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/" + getString(R.string.app_name)
            )
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val outputStream = contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    mainBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                }
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                GlobalScope.launch(Dispatchers.Main) {
                    binding.flash.visibility = View.VISIBLE
                    delay(100)
                    binding.flash.visibility = View.GONE
                }
            }
        } else {
            val imageFileFolder = File(
                Environment.getExternalStorageDirectory().toString() + '/' + getString(
                    R.string.app_name
                )
            )
            if (!imageFileFolder.exists()) {
                imageFileFolder.mkdir()
            }
            val imageName = "${System.currentTimeMillis()}.jpg"
            val imageFile = File(imageFileFolder, imageName)
            val outputStream = FileOutputStream(imageFile)
            mainBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            GlobalScope.launch(Dispatchers.Main) {
                binding.flash.visibility = View.VISIBLE
                delay(100)
                binding.flash.visibility = View.GONE
            }
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
        val texture = binding.textureView.surfaceTexture
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

    private fun updateAspectRatio(
        mode: ScreenSizeMode,
        previewSize: Size,
        isDimensionSwapped: Boolean
    ) {
        val displaySize = Point()
        ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)

        mode.setAspectRatio(binding.textureView, displaySize, previewSize, isDimensionSwapped)
    }

    private fun setupPreviewSize(camera: CameraService, isDimensionSwapped: Boolean): Size {
        val largest = camera.getCaptureSize(viewModel.screenSizeMode().comparator())
        val displaySize = Point()
        ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)

        return if (isDimensionSwapped)
            camera.getOptimalPreviewSize(
                binding.root.height,
                binding.root.width,
                displaySize.y,
                displaySize.x,
                largest,
                viewModel.dlssIsOn()
            ) else
            camera.getOptimalPreviewSize(
                binding.root.width,
                binding.root.height,
                displaySize.x,
                displaySize.y,
                largest,
                viewModel.dlssIsOn()
            )
    }

    private fun isDimensionSwapped(): Boolean {
        val displayRotation = ContextCompat.getDisplayOrDefault(requireActivity()).rotation
        val sensorOrientation =
            cameraManager!!.getCameraCharacteristics(viewModel.currentCameraId().toString())
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

    override fun showSticker(drawableId: Int) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LayoutParams(400, 400, Gravity.CENTER)
            setImageResource(drawableId)
        }
        var xDown = 0f
        var yDown = 0f
        var doubleClick = false
        imageView.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                xDown = event.x
                yDown = event.y
            }

            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (doubleClick) {
                    doubleClick = false
                    binding.stickersLayout.removeView(v)
                }
                doubleClick = true
                Handler().postDelayed({ doubleClick = false }, 400)
            }

            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                val movedX = event.x
                val movedY = event.y

                val distanceX = movedX - xDown
                val distanceY = movedY - yDown

                v.x += distanceX
                v.y += distanceY
            }

            true
        }
        binding.stickersLayout.addView(imageView)
    }
}

interface ManageCamera {
    fun openCamera(camera: CameraService)
    fun makePhoto()
    fun startBackgroundThread()
    fun stopBackgroundThread()
    fun createCameraPreviewSession(cameraDevice: CameraDevice)
}