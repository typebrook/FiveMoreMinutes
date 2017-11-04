package io.typebrook.fmmcore.redux

import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Display
import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val mapStates: List<MapState> = emptyList(),
        val currentMapNum: Int = 0,

        val currentTarget: CameraState = CameraState(),
        val cameraSave: Boolean = true,

        val display: Display = Display.Google

) : StateType

data class MapState(
        val mapControl: MapControl,
        val tile: String? = null
) : StateType

data class CameraState(
        val lat: Double = 23.76,
        val lon: Double = 120.96,
        val zoom: Float = 6f
) : StateType