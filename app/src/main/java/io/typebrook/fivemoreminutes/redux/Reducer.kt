package io.typebrook.fivemoreminutes.redux

import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

fun reducer(action: Action, oldState: State?): State {

    val state = oldState ?: State()

    return when (action) {
        is CameraPositionChange -> {
            state.copy(cameraState = action.cameraState)
        }

        is CameraPositionSave -> {
            if (!state.saveState) return state.copy(saveState = true)
            val CameraStateStack = state.previousCameraStates.subList(0, state.cameraStatePos + 1)
            state.copy(previousCameraStates = CameraStateStack + action.cameraState,
                    cameraStatePos = state.cameraStatePos + 1)
        }

        is CameraPositionReturn -> {
            if (state.cameraStatePos == 0) return state
            state.copy(cameraState = state.previousCameraStates[state.cameraStatePos - 1],
                    cameraStatePos = state.cameraStatePos - 1,
                    saveState = false)
        }

        else -> state
    }
}
