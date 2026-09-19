package com.gsfilter.filter.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * CameraX adapter for a filter pipeline. The listener owns the [ImageProxy]
 * and must close it after synchronous or asynchronous processing completes.
 */
class FilterCameraAnalyzer(
    private val onFrame: (ImageProxy) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            onFrame(image)
        } catch (error: RuntimeException) {
            image.close()
            throw error
        }
    }
}
