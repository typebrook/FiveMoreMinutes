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
            state.copy(cameraState = CameraState(lat = action.lat, lon = action.lon, zoom = action.zoom, moveMap = false))
        }
        else -> state
    }
}
