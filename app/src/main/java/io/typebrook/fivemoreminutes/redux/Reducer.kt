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
            state.copy(currentTarget = action.cameraState)
        }

        is CameraPositionSave -> {
            val cameraStateStack = state.previousCameraStates.subList(0, state.cameraStatePos + 1)
            if (!state.cameraSave || cameraStateStack.last() == action.cameraState) return state
            Log.d("states", "---------->saved ${state.cameraStatePos + 1}")
            state.copy(previousCameraStates = cameraStateStack + action.cameraState,
                    cameraStatePos = state.cameraStatePos + 1)
        }

        is CameraPositionBackward -> {
            if (state.cameraStatePos == 0) return state
            Log.d("states", "back to ${state.cameraStatePos - 1}")
            state.copy(cameraStatePos = state.cameraStatePos - 1, cameraSave = false)
        }

        is GrantCameraSave -> {
            state.copy(cameraSave = true)
        }

        is SetDisplay -> {
            state.copy(display = action.display)
        }

        is SetTile -> {
            state.copy(tileUrl = action.tileUrl)
        }

        else -> state
    }
}
