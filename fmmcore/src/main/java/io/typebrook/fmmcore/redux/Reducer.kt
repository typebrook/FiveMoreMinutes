package io.typebrook.fmmcore.redux

import android.util.Log
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

fun reducer(action: Action, oldState: State?): State {

    val state = oldState ?: State()

    return when (action) {
        is AddMap -> state.copy(mapStates = state.mapStates + MapState(mapControl = action.map))
        is RemoveMap -> state.copy(
                currentMapNum = 0,
                mapStates = state.mapStates.filter { it.mapControl != action.map })
        is SwitchMap -> state.copy(currentMapNum = (state.currentMapNum + 1) % state.mapStates.size)
        is FocusMap -> state.copy(currentMapNum = state.indexOf(action.map))

        is GrantCameraSave -> state.copy(cameraSave = true)
        is BlockCameraSave -> state.copy(cameraSave = false)

        is UpdateCurrentTarget -> {
            Log.d("Pham", state.currentCamera.toString())
            state.copy(currentCamera = action.camera)
        }

        is SetDisplay -> state.copy(display = action.display)
        is SetProjection -> state.copy(datum = action.coordSystem)

        else -> state
    }
}
