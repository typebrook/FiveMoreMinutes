package io.typebrook.fivemoreminutes.redux

import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val cameraState: CameraState = CameraState()
) : StateType

data class CameraState(
        val lat: Double = 23.76,
        val lon: Double = 120.96,
        val zoom: Float = 7.5f
) : StateType