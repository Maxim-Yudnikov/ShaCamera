package com.maxim.shacamera.camera.presentation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.R
import com.maxim.shacamera.camera.data.ComparableByArea
import com.maxim.shacamera.camera.data.ComparableByRatio
import com.maxim.shacamera.camera.data.ScreenSizeMode
import com.maxim.shacamera.core.presentation.BaseFragment
import com.maxim.shacamera.databinding.FragmentCameraBinding
import java.io.File
import java.io.FileOutputStream

class CameraFragment : BaseFragment<FragmentCameraBinding, CameraViewModel>(), ManageCamera {
    override val viewModelClass = CameraViewModel::class.java
    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCameraBinding.inflate(inflater, container, false)

    private var backgroundThread: HandlerThread? = null
    private var handler: Handler? = null

    private val myCameras = mutableListOf<CameraService>()
    private var cameraId = ""

    private var cameraManager: CameraManager? = null

    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null

    private var sizeMode = ScreenSizeMode.WIDTH_MAX
    private var screenRatio: Double = 1.0

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            viewModel.openCamera(this@CameraFragment)
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            val bitmap = binding.textureView.bitmap!!
            val width = (binding.textureView.width / viewModel.bitmapZoom()).toInt()
            val height = (binding.textureView.height / viewModel.bitmapZoom()).toInt()
            val newBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - width / 2,
                bitmap.height / 2 - height / 2,
                width,
                height
            )
            binding.imageView.setImageBitmap(newBitmap)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = false
    }


    private val zoomListener = View.OnTouchListener { v, event ->
        viewModel.handleZoom(
            cameraManager!!.getCameraCharacteristics(cameraId),
            event!!,
            listOf(binding.textureView.width, binding.textureView.height).min(),
            binding.zoomValueTextView,
            captureRequestBuilder!!,
            cameraCaptureSession!!,
            handler!!
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        myCameras.clear()
        myCameras.addAll(cameraManager!!.cameraIdList.map { id ->
            CameraService.Base(id, cameraManager!!, this)
        })

        binding.photoButton.setOnClickListener {
            viewModel.makePhoto(this)
        }

        binding.changeCameraButton.setOnClickListener {
            viewModel.changeCamera(this, cameraId.toInt())
        }

        binding.changeSizeCameraButton.setOnClickListener {
            sizeMode = when (sizeMode) {
                ScreenSizeMode.WIDTH_MAX -> ScreenSizeMode.FULL_SCREEN
                ScreenSizeMode.FULL_SCREEN -> ScreenSizeMode.SQUARE
                else -> ScreenSizeMode.WIDTH_MAX
            }
            if (sizeMode == ScreenSizeMode.FULL_SCREEN) {
                val displaySize = Point()
                ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)
                screenRatio = displaySize.let { it.y.toDouble().div(it.x) }
            } else if (sizeMode == ScreenSizeMode.SQUARE) {
                screenRatio = 4 / 3.0
            }
            viewModel.restart(this)
        }

        binding.textureView.setOnTouchListener(zoomListener)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume(this)

        if (!binding.textureView.isAvailable)
            binding.textureView.surfaceTextureListener = textureListener
        else {
            viewModel.openCamera(this)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause(this)
    }

    override fun closeCamera(id: Int) {
        myCameras[id].closeCamera()
    }

    override fun openCamera(id: Int) {
        myCameras[id].openCamera(handler!!)
        //todo need?
        cameraId = myCameras[id].cameraId()
    }

    override fun makePhoto() {
        val bitmap = binding.imageView.drawable.toBitmap()
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                }
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_LONG).show()
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_LONG).show()
        }
    }

    override fun startBackgroundThread() {
        backgroundThread = HandlerThread("camera-background")
        backgroundThread!!.start()
        handler = Handler(backgroundThread!!.looper)
    }

    override fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            handler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun createCameraPreviewSession(cameraDevice: CameraDevice) {
        val texture = binding.textureView.surfaceTexture
        val surface = Surface(texture)

        val isDimensionSwapped = isDimensionSwapped()
        val size = setupPreviewSize(myCameras[cameraId.toInt()], isDimensionSwapped)
        texture!!.setDefaultBufferSize(size.width, size.height)
        updateAspectRatio(
            sizeMode,
            setupPreviewSize(myCameras[cameraId.toInt()], isDimensionSwapped),
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

        when (mode) {
            ScreenSizeMode.FULL_SCREEN -> {

                if (isDimensionSwapped)
                    binding.textureView.setAspectRatio(displaySize.x, displaySize.y)
                else
                    binding.textureView.setAspectRatio(displaySize.y, displaySize.x)
            }

            ScreenSizeMode.WIDTH_MAX -> {
                if (isDimensionSwapped)
                    binding.textureView.setAspectRatio(previewSize.height, previewSize.width)
                else
                    binding.textureView.setAspectRatio(previewSize.width, previewSize.height)
            }

            ScreenSizeMode.SQUARE -> {
                val size = listOf(displaySize.x, displaySize.y).min()
                if (isDimensionSwapped)
                    binding.textureView.setAspectRatio(size / 4 * 3, size)
                else
                    binding.textureView.setAspectRatio(size, size / 4 * 3)
            }
        }
    }

    private fun setupPreviewSize(camera: CameraService, isDimensionSwapped: Boolean): Size {
        val comparator =
            if (sizeMode == ScreenSizeMode.WIDTH_MAX) ComparableByArea() else ComparableByRatio(
                screenRatio
            )
        val largest = camera.getCaptureSize(comparator)
        val displaySize = Point()
        ContextCompat.getDisplayOrDefault(requireActivity()).getRealSize(displaySize)

        return if (isDimensionSwapped)
            camera.getOptimalPreviewSize(
                binding.root.height,
                binding.root.width,
                displaySize.y,
                displaySize.x,
                largest
            ) else
            camera.getOptimalPreviewSize(
                binding.root.width,
                binding.root.height,
                displaySize.x,
                displaySize.y,
                largest
            )
    }

    private fun isDimensionSwapped(): Boolean {
        val displayRotation = ContextCompat.getDisplayOrDefault(requireActivity()).rotation
        val sensorOrientation = cameraManager!!.getCameraCharacteristics("0") //todo
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
}

interface ManageCamera {
    fun closeCamera(id: Int)
    fun openCamera(id: Int)
    fun makePhoto()
    fun startBackgroundThread()
    fun stopBackgroundThread()
    fun createCameraPreviewSession(cameraDevice: CameraDevice)
}