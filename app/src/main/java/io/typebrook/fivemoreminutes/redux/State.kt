package io.typebrook.fivemoreminutes.redux

import io.typebrook.fivemoreminutes.mapfragment.Display
import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val currentTarget: CameraState = CameraState(),
        val previousCameraStates: List<CameraState> = mutableListOf(CameraState()),
        val cameraStatePos: Int = 0,
        val cameraSave: Boolean = true,

        val display: Display = Display.Google,

        val tileUrl: String? = null
) : StateType

data class CameraState(
        val lat: Double = 23.76,
        val lon: Double = 120.96,
        val zoom: Float = 6f
) : StateType