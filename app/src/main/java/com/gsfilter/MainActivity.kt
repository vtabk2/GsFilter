package com.gsfilter

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.os.Bundle
import android.util.Log
import android.media.ImageReader
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gsfilter.databinding.ActivityMainBinding
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.view.FilterControlsView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var selectedControlTab = FilterControlsView.ControlTab.Filter
    private var renderedBitmap: Bitmap? = null
    private var isSaving = false
    private var isCameraMode = false
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var hasFrontCamera = false
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var cameraOpening = false
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null
    private var cameraRawSurface: Surface? = null
    private var cameraFilteredSurface: Surface? = null
    private var cameraFrameReader: ImageReader? = null
    private var cameraMakeupFeatures: MakeupFeatures? = null
    private var cameraRotationDegrees = 0
    private var cameraDetectionGeneration = 0L
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startCameraMode()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindFilterControls()
        bindBeautyControls()
        bindAdjustControls()
        binding.nextImageButton.setOnClickListener { viewModel.nextImage() }
        binding.cameraButton.setOnClickListener {
            if (isCameraMode) stopCameraMode() else requestCameraPermission()
        }
        binding.switchCameraButton.setOnClickListener { switchCamera() }
        binding.cameraPreview.surfaceTextureListener = cameraSurfaceTextureListener
        collectState()
    }

    override fun onResume() {
        super.onResume()
        binding.filterPreview.onResume()
        if (isCameraMode) {
            startCameraResources()
        }
    }

    override fun onPause() {
        if (isCameraMode) {
            stopCameraResources()
        }
        binding.filterPreview.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopCameraMode()
        super.onDestroy()
    }

    private fun bindFilterControls() {
        binding.filterControls.onCloseClick = ::saveFilteredImage
        binding.filterControls.onControlTabSelected = ::selectControlTab
        binding.filterControls.onCategorySelected = viewModel::selectCategory
        binding.filterControls.onCategoryAutoSelected = viewModel::autoSelectCategory
        binding.filterControls.onFilterSelected = viewModel::selectFilter
        binding.filterControls.onFilterIntensityChanged = viewModel::setFilterIntensity
        renderControlTabs()
    }

    private fun selectControlTab(tab: FilterControlsView.ControlTab) {
        selectedControlTab = tab
        renderControlTabs()
    }

    private fun renderControlTabs() {
        binding.filterControls.setSelectedTab(selectedControlTab)
    }

    private fun bindAdjustControls() {
        binding.filterControls.onAdjustmentChanged = viewModel::setAdjustment
        binding.filterControls.onResetAllAdjustClick = viewModel::resetAdjustments
    }

    private fun bindBeautyControls() {
        binding.filterControls.onBeautyChanged = { control, value ->
            when (control) {
                FilterControlsView.BeautyControl.Smoothing -> viewModel.setSkinSmoothing(value)
                FilterControlsView.BeautyControl.Whitening -> viewModel.setSkinWhitening(value)
                FilterControlsView.BeautyControl.Blush -> viewModel.setBlush(value)
                FilterControlsView.BeautyControl.Lipstick -> viewModel.setLipstick(value)
                FilterControlsView.BeautyControl.UnderEye -> viewModel.setUnderEye(value)
                FilterControlsView.BeautyControl.TeethWhitening -> viewModel.setTeethWhitening(value)
                FilterControlsView.BeautyControl.EyeShadow -> viewModel.setEyeShadow(value)
                FilterControlsView.BeautyControl.Eyeliner -> viewModel.setEyeliner(value)
                FilterControlsView.BeautyControl.Eyebrow -> viewModel.setEyebrow(value)
                FilterControlsView.BeautyControl.FaceSlimming -> viewModel.setFaceSlimming(value)
                FilterControlsView.BeautyControl.EyeEnlargement -> viewModel.setEyeEnlargement(value)
            }
        }
        binding.filterControls.onResetBeautyClick = viewModel::resetBeauty
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: FilterUiState) {
        binding.progressBar.isVisible = state.isLoading || isSaving
        binding.nextImageButton.isEnabled = state.imageAssetCount > 1 && !state.isLoading && !isSaving
        binding.cameraButton.isEnabled = !state.isLoading && !isSaving
        binding.errorText.isVisible = state.error != null
        binding.errorText.text = state.error?.toMessage().orEmpty()
        val selectedRecipe = state.selectedRecipe
        if (renderedBitmap !== state.sourceBitmap) {
            renderedBitmap = state.sourceBitmap
            binding.imageOriginal.setImageBitmap(state.sourceBitmap)
            binding.filterPreview.setSourceBitmap(state.sourceBitmap)
        }
        binding.filterPreview.setFilterState(
            recipe = selectedRecipe,
            adjustments = state.adjustments,
            makeupFeatures = if (isCameraMode) cameraMakeupFeatures else state.makeupFeatures,
        )
        binding.filterControls.setState(
            selectedCategory = state.selectedCategory,
            selectedFilter = state.selectedFilter,
            thumbnailBitmap = state.sourceBitmap,
            thumbnailKey = state.filterThumbnailKey,
            selectedRecipe = selectedRecipe,
        )
        binding.filterControls.setAdjustments(state.adjustments)
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraMode()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraMode() {
        if (isCameraMode) {
            return
        }
        isCameraMode = true
        updateCameraAvailability()
        binding.imageOriginal.isVisible = false
        binding.cameraPreview.isVisible = true
        binding.nextImageButton.isVisible = false
        binding.cameraButton.contentDescription = getString(R.string.stop_camera)
        updateCameraSwitchButton()
        val state = viewModel.state.value
        binding.filterPreview.setFilterState(
            recipe = state.selectedRecipe,
            adjustments = state.adjustments,
            makeupFeatures = null,
        )
        startCameraResources()
    }

    private fun stopCameraMode() {
        if (!isCameraMode) {
            return
        }
        isCameraMode = false
        stopCameraResources()
        binding.filterPreview.setCameraSource(false)
        binding.cameraPreview.isVisible = false
        binding.imageOriginal.isVisible = true
        binding.nextImageButton.isVisible = true
        binding.cameraButton.contentDescription = getString(R.string.use_camera)
        updateCameraSwitchButton()
        val state = viewModel.state.value
        binding.filterPreview.setSourceBitmap(state.sourceBitmap)
        binding.filterPreview.setFilterState(
            recipe = state.selectedRecipe,
            adjustments = state.adjustments,
            makeupFeatures = state.makeupFeatures,
        )
    }

    private fun startCameraResources() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        startCameraThread()
        val handler = cameraHandler ?: return
        val detectionGeneration = ++cameraDetectionGeneration
        cameraFrameReader = ImageReader.newInstance(
            CAMERA_ANALYSIS_WIDTH,
            CAMERA_ANALYSIS_HEIGHT,
            ImageFormat.YUV_420_888,
            2,
        ).apply {
            setOnImageAvailableListener({ reader ->
                val image = try {
                    reader.acquireLatestImage()
                } catch (_: IllegalStateException) {
                    null
                } ?: return@setOnImageAvailableListener
                if (!isCameraMode || detectionGeneration != cameraDetectionGeneration) {
                    image.close()
                    return@setOnImageAvailableListener
                }
                viewModel.detectCameraFrame(image, cameraRotationDegrees) detector@{ features ->
                    if (!isCameraMode || detectionGeneration != cameraDetectionGeneration) {
                        return@detector
                    }
                    cameraMakeupFeatures = features
                    val state = viewModel.state.value
                    binding.filterPreview.setFilterState(
                        recipe = state.selectedRecipe,
                        adjustments = state.adjustments,
                        makeupFeatures = features,
                    )
                }
            }, handler)
        }
        binding.filterPreview.setCameraSource(true) { surface ->
            cameraFilteredSurface = surface
            if (surface == null) {
                showCameraError(null)
            } else {
                openCameraIfReady()
            }
        }
        openCameraIfReady()
    }

    private fun switchCamera() {
        if (!isCameraMode || !hasFrontCamera) {
            return
        }
        val nextFacing = if (
            cameraFacing == CameraCharacteristics.LENS_FACING_BACK
        ) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val manager = getSystemService(CameraManager::class.java)
        val cameraExists = try {
            findCameraId(manager, nextFacing) != null
        } catch (error: CameraAccessException) {
            showCameraError(error)
            return
        }
        if (!cameraExists) {
            return
        }
        cameraFacing = nextFacing
        updateCameraSwitchButton()
        stopCameraResources()
        startCameraResources()
    }

    private fun updateCameraAvailability() {
        val manager = getSystemService(CameraManager::class.java)
        hasFrontCamera = try {
            findCameraId(manager, CameraCharacteristics.LENS_FACING_FRONT) != null
        } catch (_: CameraAccessException) {
            false
        }
    }

    private fun updateCameraSwitchButton() {
        binding.switchCameraButton.isVisible = isCameraMode && hasFrontCamera
        binding.switchCameraButton.contentDescription = getString(
            if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                R.string.use_front_camera
            } else {
                R.string.use_back_camera
            },
        )
    }

    private fun stopCameraResources() {
        cameraDetectionGeneration++
        cameraMakeupFeatures = null
        cameraFrameReader?.close()
        cameraFrameReader = null
        cameraSession?.close()
        cameraSession = null
        cameraDevice?.close()
        cameraDevice = null
        cameraOpening = false
        cameraRawSurface?.release()
        cameraRawSurface = null
        cameraFilteredSurface = null
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }

    private fun startCameraThread() {
        if (cameraThread != null) {
            return
        }
        cameraThread = HandlerThread("GsFilterCamera").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    @SuppressLint("MissingPermission")
    private fun openCameraIfReady() {
        if (
            !isCameraMode ||
            cameraDevice != null ||
            cameraOpening ||
            cameraFilteredSurface == null ||
            !binding.cameraPreview.isAvailable
        ) {
            return
        }
        val handler = cameraHandler ?: return
        val texture = binding.cameraPreview.surfaceTexture ?: return
        val manager = getSystemService(CameraManager::class.java)
        val cameraId = try {
            findCameraId(manager, cameraFacing)
                ?: if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    manager.cameraIdList.firstOrNull()
                } else {
                    null
                }
        } catch (error: CameraAccessException) {
            showCameraError(error)
            return
        }
        if (cameraId == null) {
            showCameraError(null)
            return
        }

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val displayRotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        cameraRotationDegrees = if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            (sensorOrientation + displayRotation) % 360
        } else {
            (sensorOrientation - displayRotation + 360) % 360
        }
        texture.setDefaultBufferSize(CAMERA_WIDTH, CAMERA_HEIGHT)
        cameraRawSurface?.release()
        cameraRawSurface = Surface(texture)
        cameraOpening = true
        try {
            manager.openCamera(cameraId, cameraStateCallback, handler)
        } catch (error: CameraAccessException) {
            cameraOpening = false
            showCameraError(error)
        } catch (error: SecurityException) {
            cameraOpening = false
            showCameraError(error)
        }
    }

    private fun findCameraId(manager: CameraManager, lensFacing: Int): String? =
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                lensFacing
        }

    private val cameraSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (isCameraMode) {
                openCameraIfReady()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            if (isCameraMode) {
                stopCameraResources()
            }
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            runOnUiThread {
                cameraOpening = false
                if (
                    !isCameraMode ||
                    !binding.cameraPreview.isAvailable ||
                    cameraRawSurface == null ||
                    cameraFilteredSurface == null
                ) {
                    device.close()
                    return@runOnUiThread
                }
                cameraDevice = device
                createCameraSession(device)
            }
        }

        override fun onDisconnected(device: CameraDevice) {
            runOnUiThread {
                device.close()
                if (cameraDevice === device) {
                    cameraDevice = null
                }
            }
        }

        override fun onError(device: CameraDevice, error: Int) {
            runOnUiThread {
                device.close()
                if (cameraDevice === device) {
                    cameraDevice = null
                }
                showCameraError(null)
            }
        }
    }

    private fun createCameraSession(device: CameraDevice) {
        val rawSurface = cameraRawSurface ?: return
        val filteredSurface = cameraFilteredSurface ?: return
        val frameReaderSurface = cameraFrameReader?.surface
        try {
            device.createCaptureSession(
                listOfNotNull(rawSurface, filteredSurface, frameReaderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        runOnUiThread {
                            if (!isCameraMode || cameraDevice !== device) {
                                session.close()
                                return@runOnUiThread
                            }
                            cameraSession = session
                            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(rawSurface)
                                addTarget(filteredSurface)
                                frameReaderSurface?.let { addTarget(it) }
                                set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                                )
                            }
                            session.setRepeatingRequest(request.build(), null, cameraHandler)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        runOnUiThread { showCameraError(null) }
                    }
                },
                cameraHandler,
            )
        } catch (error: CameraAccessException) {
            showCameraError(error)
        }
    }

    private fun showCameraError(error: Exception?) {
        Log.e(TAG, "Camera preview failed", error)
        runOnUiThread {
            if (isCameraMode) {
                Toast.makeText(this, R.string.camera_open_failed, Toast.LENGTH_SHORT).show()
                stopCameraMode()
            }
        }
    }

    private fun saveFilteredImage() {
        if (isSaving) {
            return
        }

        lifecycleScope.launch {
            setSaving(true)
            val file = try {
                val bitmap = viewModel.renderFilteredBitmap()
                if (bitmap == null) {
                    null
                } else {
                    Log.d("BitmapCompare", "source/result same=${areBitmapsEqual(viewModel.state.value.sourceBitmap, bitmap)}")
                    try {
                        withContext(Dispatchers.IO) { saveToAppStorage(bitmap) }
                    } finally {
                        bitmap.recycle()
                    }
                }
            } catch (error: RuntimeException) {
                if (error is CancellationException) {
                    throw error
                }
                null
            } catch (_: IOException) {
                null
            } finally {
                setSaving(false)
            }

            Toast.makeText(
                this@MainActivity,
                if (file == null) R.string.image_save_failed else R.string.image_saved_to_app_storage,
                Toast.LENGTH_SHORT,
            ).show()
//            if (file != null) {
//                finish()
//            }
        }
    }

    private fun setSaving(saving: Boolean) {
        isSaving = saving
        binding.progressBar.isVisible = viewModel.state.value.isLoading || saving
    }

    private fun areBitmapsEqual(first: Bitmap?, second: Bitmap?): Boolean =
        when {
            first === second -> true
            first == null || second == null -> false
            first.width != second.width || first.height != second.height -> false
            else -> first.sameAs(second)
        }

    private fun saveToAppStorage(bitmap: Bitmap): File {
        Log.d("TAG5", "saveToAppStorage: bitmap.width = " + bitmap.width)
        Log.d("TAG5", "saveToAppStorage: bitmap.height = " + bitmap.height)
        val directory = File(filesDir, FILTERED_IMAGES_DIR)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create filtered images directory.")
        }

        val file = File.createTempFile(FILTERED_IMAGE_PREFIX, FILTERED_IMAGE_SUFFIX, directory)
        var saved = false
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IOException("Cannot encode filtered image.")
                }
            }
            saved = true
            return file
        } finally {
            if (!saved) {
                file.delete()
            }
        }
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }

    private companion object {
        const val TAG = "GsFilterCamera"
        const val CAMERA_WIDTH = 640
        const val CAMERA_HEIGHT = 480
        const val CAMERA_ANALYSIS_WIDTH = 480
        const val CAMERA_ANALYSIS_HEIGHT = 360
        const val FILTERED_IMAGES_DIR = "filtered"
        const val FILTERED_IMAGE_PREFIX = "filtered_"
        const val FILTERED_IMAGE_SUFFIX = ".jpg"
        const val JPEG_QUALITY = 95
    }
}
