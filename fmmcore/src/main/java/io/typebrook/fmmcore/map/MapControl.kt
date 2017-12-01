package io.typebrook.fmmcore.map

import io.typebrook.fmmcore.projection.XYPair
import io.typebrook.fmmcore.redux.CameraState

/**
 * Created by pham on 2017/10/16.
 * interface for redux subscriber
 */

interface MapControl {

    val cameraState: CameraState
    val screenBound: Pair<XYPair, XYPair>

    var cameraQueue: List<CameraState>
    var cameraStatePos: Int

    val styles: List<Tile>

    fun moveCamera(target: CameraState)
    fun animateCamera(target: CameraState, duration: Int)
    fun zoomBy(value: Float)

    fun changeStyle(style: Tile.PrivateStyle?)
    fun changeWebTile(tile: Tile.WebTile?)
}

sealed class Tile(val name: String) {
    class WebTile(name: String, val url: String) : Tile(name)
    class PrivateStyle(name: String, val value: Any) : Tile(name)
}

infix fun String.fromWebTile(url: String): Tile.WebTile = Tile.WebTile(this, url)
infix fun String.fromStyle(style: Any): Tile.PrivateStyle = Tile.PrivateStyle(this, style)
