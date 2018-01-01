package io.typebrook.fmmcore.redux

import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

fun reducer(action: Action, oldState: State?): State {

    val state = oldState ?: State()

    return when (action) {
        is AddMap -> state.copy(maps = state.maps + MapInfo(mapControl = action.map))
        is RemoveMap -> state.copy(currentMapNum = 0, maps = state.maps.filter { it.mapControl != action.map })
        is SwitchMap -> state.copy(currentMapNum = (state.currentMapNum + 1) % state.maps.size)
        is FocusMap -> state.copy(currentMapNum = state.indexOf(action.map))
        is DidSwitchLocation -> state.copy(maps = state.maps.map {
            if (it.mapControl == action.map) it.copy(locating = action.isEnabled) else it
        })

        is GrantCameraSave -> state.copy(cameraSave = true)
        is BlockCameraSave -> state.copy(cameraSave = false)

        is UpdateCurrentTarget -> state.copy(currentCamera = action.camera)

        is SetDisplay -> state.copy(display = action.display)
        is SetProjection -> state.copy(crs = action.coordSystem)

        else -> state
    }
}
