package io.typebrook.fivemoreminutes.redux

import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val cameraState: CameraState = CameraState()
) : StateType

data class CameraState(
        val lat: Double = 24.782347,
        val lon: Double = 121.035044,
        val zoom: Float = 3f
) : StateType