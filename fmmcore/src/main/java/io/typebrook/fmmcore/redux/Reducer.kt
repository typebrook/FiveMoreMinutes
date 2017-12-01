package io.typebrook.fmmcore.redux

import android.util.Log
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

fun reducer(action: Action, oldState: State?): State {

    val state = oldState ?: State()

    return when (action) {
        is AddMap, is RemoveMap, is SwitchMap, is FocusMap ->
            state.copy(mapState = mapReducer(action, state.mapState))

        is GrantCameraSave -> state.copy(cameraSave = true)
        is BlockCameraSave -> state.copy(cameraSave = false)

        is UpdateCurrentTarget -> state.copy(currentCamera = action.camera)

        is SetDisplay -> state.copy(display = action.display)
        is DidFinishSetTile -> state.apply {
            Log.d("Wow", action.tile.toString())
        }
        is SetProjection -> state.copy(datum = action.coordSystem)

        else -> state
    }
}

fun mapReducer(action: Action, oldState: MapState?): MapState {

    val state = oldState ?: MapState()

    return when (action) {
        is AddMap -> state.copy(maps = state.maps + MapInfo(mapControl = action.map))
        is RemoveMap -> state.copy(currentMapNum = 0, maps = state.maps.filter { it.mapControl != action.map })
        is SwitchMap -> state.copy(currentMapNum = (state.currentMapNum + 1) % state.maps.size)
        is FocusMap -> state.copy(currentMapNum = state.indexOf(action.map))

        else -> state
    }
}
