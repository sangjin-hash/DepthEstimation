/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shubham0204.ml.depthestimation

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.shubham0204.ml.depthestimation.util.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.checkerframework.checker.signedness.qual.Unsigned

// Image Analyser for performing depth estimation on camera frames.
class FrameAnalyser(
    private var depthEstimationModel : MiDASModel ,
    private var drawingOverlay: DrawingOverlay ) : ImageAnalysis.Analyzer {

    private var frameBitmap : Bitmap? = null
    private var isFrameProcessing = false
    var isComputingDepthMap = false

    var depthMapArray: UByteArray? = null


    override fun analyze(image: ImageProxy) {
        // Return if depth map computation is turned off. See MainActivity.kt
        if ( !isComputingDepthMap ) {
            image.close()
            return
        }
        // If a frame is being processed, drop the current frame.
        if ( isFrameProcessing ) {
            image.close()
            return
        }
        isFrameProcessing = true
        if ( image.image != null ) {
            // Get the `Bitmap` of the current frame ( with corrected rotation ).
            frameBitmap = BitmapUtils.imageToBitmap( image.image!! , image.imageInfo.rotationDegrees )
            image.close()
            CoroutineScope( Dispatchers.Main ).launch {
                runModel( frameBitmap!! )
            }
        }
    }


    private suspend fun runModel( inputImage : Bitmap ) = withContext( Dispatchers.Default ) {
        // Compute the depth given the frame Bitmap.
        val output = depthEstimationModel.getDepthMap( inputImage )

        /**
         * output.config = RGB_565
         * Model output: 256 x 256
         * resize 후: 720 x 1280
         * Bitmap -> Unsigned Byte(8bit) array
         */
        val resize_output = BitmapUtils.resizeBitmap( output ,
            frameBitmap!!.width , frameBitmap!!.height)

        val x = resize_output.width
        val y = resize_output.height
        var pixels = IntArray(x*y)
        resize_output.getPixels(pixels, 0, x, 0, 0, x, y);

        depthMapArray = UByteArray(pixels.size)
        for(i in pixels.indices) {
            val pixel = pixels[i]
            val grayScale =
                0.299 * (pixel shr 16 and 0xFF) + 0.587 * (pixel shr 8 and 0xFF) + 0.114 * (pixel and 0xFF)
            depthMapArray!![i] = grayScale.toInt().toUByte()
        }

        withContext( Dispatchers.Main ) {
            // Notify that the current frame is processed and the pipeline is
            // ready for the next frame.
            isFrameProcessing = false
            if ( drawingOverlay.visibility == View.GONE ) {
                drawingOverlay.visibility = View.VISIBLE
            }
            // Submit the depth Bitmap to the DrawingOverlay and update it.
            // Note, calling `drawingOverlay.invalidate()` here will call `onDraw()` in DrawingOverlay.kt.
            drawingOverlay.depthMaskBitmap = BitmapUtils.resizeBitmap( output ,
                frameBitmap!!.width , frameBitmap!!.height)
            drawingOverlay.invalidate()
        }
    }

}