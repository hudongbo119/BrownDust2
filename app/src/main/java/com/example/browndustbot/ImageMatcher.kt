package com.example.browndustbot

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

data class MatchResult(
    val found: Boolean,
    val centerX: Int = 0,
    val centerY: Int = 0,
    val confidence: Double = 0.0,
    val matchRect: Rect? = null
)

class ImageMatcher {

    companion object {
        private const val TAG = "ImageMatcher"
        private const val DEFAULT_THRESHOLD = 0.85
    }

    fun findTemplate(
        screenBitmap: Bitmap,
        templateBitmap: Bitmap,
        threshold: Double = DEFAULT_THRESHOLD
    ): MatchResult {
        val screenMat = Mat()
        val templateMat = Mat()
        val screenGray = Mat()
        val templateGray = Mat()
        val resultMat = Mat()

        return try {
            Utils.bitmapToMat(screenBitmap, screenMat)
            Utils.bitmapToMat(templateBitmap, templateMat)

            Imgproc.cvtColor(screenMat, screenGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGBA2GRAY)

            Imgproc.matchTemplate(screenGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val minMaxResult = Core.minMaxLoc(resultMat)
            val maxVal = minMaxResult.maxVal
            val maxLoc = minMaxResult.maxLoc

            if (maxVal >= threshold) {
                val centerX = (maxLoc.x + templateBitmap.width / 2).toInt()
                val centerY = (maxLoc.y + templateBitmap.height / 2).toInt()
                val matchRect = Rect(
                    maxLoc.x.toInt(),
                    maxLoc.y.toInt(),
                    (maxLoc.x + templateBitmap.width).toInt(),
                    (maxLoc.y + templateBitmap.height).toInt()
                )
                MatchResult(true, centerX, centerY, maxVal, matchRect)
            } else {
                MatchResult(false, confidence = maxVal)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findTemplate", e)
            MatchResult(false)
        } finally {
            screenMat.release()
            templateMat.release()
            screenGray.release()
            templateGray.release()
            resultMat.release()
        }
    }

    fun findAllTemplates(
        screenBitmap: Bitmap,
        templateBitmap: Bitmap,
        threshold: Double = DEFAULT_THRESHOLD
    ): List<MatchResult> {
        val screenMat = Mat()
        val templateMat = Mat()
        val screenGray = Mat()
        val templateGray = Mat()
        val resultMat = Mat()
        val results = mutableListOf<MatchResult>()

        return try {
            Utils.bitmapToMat(screenBitmap, screenMat)
            Utils.bitmapToMat(templateBitmap, templateMat)

            Imgproc.cvtColor(screenMat, screenGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGBA2GRAY)

            Imgproc.matchTemplate(screenGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val templateW = templateBitmap.width
            val templateH = templateBitmap.height
            val nmsRadius = minOf(templateW, templateH) / 2

            val mask = Mat.ones(resultMat.size(), CvType.CV_8U)

            for (y in 0 until resultMat.rows()) {
                for (x in 0 until resultMat.cols()) {
                    val confidence = resultMat.get(y, x)[0]
                    if (confidence >= threshold && mask.get(y, x)[0] > 0) {
                        val centerX = x + templateW / 2
                        val centerY = y + templateH / 2
                        val matchRect = Rect(x, y, x + templateW, y + templateH)
                        results.add(MatchResult(true, centerX, centerY, confidence, matchRect))

                        // Zero out nearby region to suppress duplicate matches (NMS)
                        val x1 = maxOf(0, x - nmsRadius)
                        val y1 = maxOf(0, y - nmsRadius)
                        val x2 = minOf(resultMat.cols() - 1, x + nmsRadius)
                        val y2 = minOf(resultMat.rows() - 1, y + nmsRadius)
                        for (ny in y1..y2) {
                            for (nx in x1..x2) {
                                mask.put(ny, nx, 0.0)
                            }
                        }
                    }
                }
            }

            mask.release()
            results.sortedByDescending { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findAllTemplates", e)
            emptyList()
        } finally {
            screenMat.release()
            templateMat.release()
            screenGray.release()
            templateGray.release()
            resultMat.release()
        }
    }
}
