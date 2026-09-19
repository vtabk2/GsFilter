package com.gsfilter.filter.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.gsfilter.filter.FilterAnalysis
import com.gsfilter.filter.GsFilterAnalyzer

/**
 * CameraX adapter for a filter pipeline. The listener owns the [ImageProxy]
 * and must close it after synchronous or asynchronous processing completes.
 */
class FilterCameraAnalyzer(
    private val onFrame: (ImageProxy) -> Unit,
) : ImageAnalysis.Analyzer {

    constructor(
        analyzer: GsFilterAnalyzer,
        onResult: (FilterAnalysis?) -> Unit,
    ) : this({ image ->
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
        } else {
            analyzer.analyze(mediaImage, image.imageInfo.rotationDegrees) { result ->
                image.use { image ->
                    onResult(result)
                }
            }
        }
    })

    override fun analyze(image: ImageProxy) {
        try {
            onFrame(image)
        } catch (error: RuntimeException) {
            image.close()
            throw error
        }
    }
}
