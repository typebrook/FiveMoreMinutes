package io.typebrook.fivemoreminutes.mapfragment

import io.typebrook.fivemoreminutes.mainStore
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/10/16.
 * interface for redux subscriber
 */

interface MapControl {
    fun addTile(tileUrl: String?)
    fun animateCamera(lat: Double, lon: Double, zoom: Float)
}

class CameraListener(private val map: MapControl) : StoreSubscriber<Int> {
    override fun newState(state: Int) {
        val (lat, lon, zoom) = mainStore.state.previousCameraStates[state]
        map.animateCamera(lat, lon, zoom)
    }
}

class TileListener(private val map: MapControl) : StoreSubscriber<String?> {
    override fun newState(state: String?) {
        map.addTile(state)
    }
}