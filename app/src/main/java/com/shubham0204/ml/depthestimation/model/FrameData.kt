package com.shubham0204.ml.depthestimation.model

import java.nio.FloatBuffer

data class FrameData(
    val points : FloatBuffer,
    val colors : FloatBuffer
)
