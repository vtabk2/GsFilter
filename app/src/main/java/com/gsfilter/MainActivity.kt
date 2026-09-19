package com.gsfilter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var selectedControlTab = FilterControlsView.ControlTab.Filter
    private var renderedBitmap: Bitmap? = null
    private var isSaving = false
    private var isCameraMode = false
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var hasFrontCamera = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var faceMeshDetector: FaceMeshDetector? = null
    private var cameraMakeupFeatures: MakeupFeatures? = null
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
        faceMeshDetector?.close()
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
        val detectionGeneration = ++cameraDetectionGeneration
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceMeshDetector?.close()
        try {
            faceMeshDetector = FaceMeshDetector(this, object : FaceMeshDetector.Listener {
                override fun onResult(timestampMs: Long, features: MakeupFeatures?) {
                    runOnUiThread {
                        if (!isCameraMode || detectionGeneration != cameraDetectionGeneration) {
                            return@runOnUiThread
                        }
                        cameraMakeupFeatures = features
                        val state = viewModel.state.value
                        binding.filterPreview.setFilterState(
                            recipe = state.selectedRecipe,
                            adjustments = state.adjustments,
                            makeupFeatures = features,
                        )
                    }
                }

                override fun onError(error: RuntimeException) {
                    Log.e(TAG, "Face Mesh failed", error)
                }
            })
        } catch (error: RuntimeException) {
            showCameraError(error)
            return
        }
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            if (!isCameraMode || detectionGeneration != cameraDetectionGeneration) {
                return@addListener
            }
            try {
                cameraProvider = providerFuture.get()
                bindCamera(cameraProvider!!, detectionGeneration)
            } catch (error: RuntimeException) {
                showCameraError(error)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        if (!isCameraMode || !hasFrontCamera) {
            return
        }
        val nextFacing = if (
            cameraFacing == CameraSelector.LENS_FACING_BACK
        ) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        cameraFacing = nextFacing
        updateCameraSwitchButton()
        stopCameraResources()
        startCameraResources()
    }

    private fun updateCameraAvailability() {
        val provider = cameraProvider
        hasFrontCamera = provider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
    }

    private fun updateCameraSwitchButton() {
        binding.switchCameraButton.isVisible = isCameraMode && hasFrontCamera
        binding.switchCameraButton.contentDescription = getString(
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                R.string.use_front_camera
            } else {
                R.string.use_back_camera
            },
        )
    }

    private fun stopCameraResources() {
        cameraDetectionGeneration++
        cameraMakeupFeatures = null
        cameraAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        cameraAnalysis = null
        cameraProvider = null
        faceMeshDetector?.close()
        faceMeshDetector = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        binding.filterPreview.clearCameraFrame()
    }

    private fun bindCamera(provider: ProcessCameraProvider, generation: Long) {
        val selector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        binding.cameraPreview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        binding.cameraPreview.scaleType = PreviewView.ScaleType.FILL_CENTER
        binding.cameraPreview.scaleX = 1f
        val preview = Preview.Builder()
            .setTargetResolution(Size(CAMERA_WIDTH, CAMERA_HEIGHT))
            .build()
            .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(CAMERA_ANALYSIS_WIDTH, CAMERA_ANALYSIS_HEIGHT))
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val executor = cameraExecutor ?: return
        analysis.setAnalyzer(executor) { image ->
            analyzeCameraFrame(image, generation)
        }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, analysis)
            cameraAnalysis = analysis
            hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            updateCameraSwitchButton()
        } catch (error: RuntimeException) {
            showCameraError(error)
        }
    }

    private fun analyzeCameraFrame(image: ImageProxy, generation: Long) {
        try {
            if (!isCameraMode || generation != cameraDetectionGeneration) {
                return
            }
            val rotationDegrees = image.imageInfo.rotationDegrees
            val timestampNanos = image.imageInfo.timestamp
            val isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            val rawBitmap = image.toRgbaBitmap()
            val displayBitmap = rotateCameraBitmap(
                rawBitmap,
                rotationDegrees,
                isFrontCamera,
            )
            if (displayBitmap !== rawBitmap) {
                rawBitmap.recycle()
            }
            faceMeshDetector?.detect(displayBitmap, timestampNanos)
            runOnUiThread {
                if (!isCameraMode || generation != cameraDetectionGeneration) {
                    displayBitmap.recycle()
                    return@runOnUiThread
                }
                binding.filterPreview.setCameraFrame(displayBitmap)
            }
        } catch (error: RuntimeException) {
            if (!error.message.orEmpty().contains("already closed", ignoreCase = true)) {
                Log.e(TAG, "Camera analysis failed", error)
            }
        } finally {
            image.closeSafely()
        }
    }

    private fun rotateCameraBitmap(source: Bitmap, rotationDegrees: Int, mirror: Boolean): Bitmap {
        if (rotationDegrees == 0 && !mirror) {
            return source
        }
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (mirror) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun ImageProxy.toRgbaBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = planes.first().buffer
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun ImageProxy.closeSafely() {
        try {
            close()
        } catch (error: IllegalStateException) {
            if (!error.message.orEmpty().contains("already closed", ignoreCase = true)) {
                throw error
            }
            Log.w(TAG, "CameraX delivered an already-closed frame")
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
