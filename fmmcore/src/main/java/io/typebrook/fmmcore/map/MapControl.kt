package io.typebrook.fmmcore.map

import io.typebrook.fmmcore.realm.projection.XYPair
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
    fun animateToBound(ne: XYPair, sw: XYPair, duration: Int)
    fun zoomBy(value: Float)

    fun setStyle(style: Tile.PrivateStyle?)
    fun setWebTile(tile: Tile.WebTile?)
    fun addWebTile(tile: Tile.WebTile)
    fun addWebImage(tile: Tile.WebImage)

    fun enableLocation()
    fun disableLocation()
}

sealed class Tile(val name: String) {
    class PrivateStyle(name: String, val value: Any) : Tile(name)
    class WebTile(name: String, val url: String, val size: Int = 128) : Tile(name)
    class WebImage(name: String, val url: String, var bound: Pair<XYPair, XYPair>? = null) : Tile(name)
}

infix fun String.fromWebTile(url: String) = Tile.WebTile(this, url)
infix fun String.fromRoughWebTile(url: String) = Tile.WebTile(this, url, 256)
infix fun String.fromWebImage(url: String) = Tile.WebImage(this, url)
infix fun Tile.WebImage.at(bound: Pair<XYPair, XYPair>) = this.apply { this.bound = bound }
infix fun String.fromStyle(style: Any) = Tile.PrivateStyle(this, style)

// Simple class fulfills interface to replace actual MapControl, used when onMapReady not yer done
class SimpleMap : MapControl {

    override val cameraState: CameraState = CameraState()
    override val screenBound: Pair<XYPair, XYPair> = (0.0 to 0.0) to (0.0 to 0.0)

    override var cameraQueue: List<CameraState> = emptyList()
    override var cameraStatePos: Int = 0

    override val styles: List<Tile> = emptyList()

    override fun moveCamera(target: CameraState) {}
    override fun animateCamera(target: CameraState, duration: Int) {}
    override fun animateToBound(ne: XYPair, sw: XYPair, duration: Int) {}
    override fun zoomBy(value: Float) {}

    override fun setStyle(style: Tile.PrivateStyle?) {}
    override fun setWebTile(tile: Tile.WebTile?) {}
    override fun addWebTile(tile: Tile.WebTile) {}
    override fun addWebImage(tile: Tile.WebImage) {}

    override fun enableLocation() {}
    override fun disableLocation() {}
}
