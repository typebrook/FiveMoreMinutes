package io.typebrook.fmmcore.redux

import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.SimpleMap
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.projection.Datum
import io.typebrook.fmmcore.projection.WGS84
import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val maps: List<MapInfo> = emptyList(),
        val currentMapNum: Int = 0,

        val currentCamera: CameraState = CameraState(),
        val cameraSave: Boolean = true,

        val display: Display = Display.MapBox,

        val crs: Datum = WGS84
) : StateType {
    val currentMap: MapInfo
        get() = if (maps.lastIndex >= currentMapNum) maps[currentMapNum] else MapInfo(SimpleMap())

    val currentControl: MapControl
        get() = currentMap.mapControl

    fun indexOf(mapControl: MapControl): Int {
        maps.mapIndexed { index, mapInfo -> if (mapInfo.mapControl == mapControl) return index }
        return currentMapNum
    }
}

data class MapInfo(
        val mapControl: MapControl,
        val locating: Boolean = false,
        val tile: Tile? = null
)

data class CameraState(
        val lat: Double = 23.76,
        val lon: Double = 120.96,
        val zoom: Float = 7f
) : StateType