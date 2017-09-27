package io.typebrook.fivemoreminutes.redux

import android.util.Log
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

fun reducer(action: Action, oldState: State?): State {

    val state = oldState ?: State()

    return when (action) {
        is CameraPositionChange -> {
            Log.d("new Camera state", "${action.lat} ${action.lon} ${action.zoom}")
            state.copy(cameraState = CameraState(lat = action.lat, lon = action.lon, zoom = action.zoom, moveMap = false))
        }

        is CameraPositionSave -> {
            Log.d("new Camera Save", state.previousCameraState.size.toString())
            state.copy(previousCameraState = state.previousCameraState.subList(0, state.cameraStatePos + 1) + CameraState(lat = action.lat, lon = action.lon, zoom = action.zoom, moveMap = true),
                    cameraStatePos = state.cameraStatePos + 1)
        }

        is CameraPositionReturn -> {
            state.copy(cameraState = state.previousCameraState[state.cameraStatePos - 1],
                    cameraStatePos = state.cameraStatePos - 1)
        }

        else -> state
    }
}
