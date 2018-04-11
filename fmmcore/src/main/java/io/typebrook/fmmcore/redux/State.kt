package io.typebrook.fmmcore.redux

import android.app.Activity
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.SimpleMap
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.realm.projection.CoordRefSys
import io.typebrook.fmmcore.realm.projection.Expression
import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val activity: Activity? = null,

        val maps: List<MapInfo> = emptyList(),
        val display: Display = Display.MapBox,
        val currentMapNum: Int = 0,
        val hideComponent: Boolean = false,

        val currentCamera: CameraState = CameraState(),
        val cameraSave: Boolean = true,
        val crsState: CrsState = CrsState(),

        val mode: Mode = Mode.Default
) : StateType {
    val currentMap get() = if (maps.lastIndex >= currentMapNum) maps[currentMapNum] else MapInfo(SimpleMap())

    val currentControl get() = currentMap.mapControl

    val currentXY get() = currentCamera.run { lon to lat }
    val currentXYZ get() = currentCamera.run { Triple(lat, lon, zoom) }

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

data class CrsState(
        val crs: CoordRefSys = CoordRefSys.WGS84,
        val isLonLat: Boolean = true,
        val coordExpr: Expression = Expression.Degree
) : StateType

enum class Mode { Default, Focus }