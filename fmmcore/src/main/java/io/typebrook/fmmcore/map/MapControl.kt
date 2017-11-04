package io.typebrook.fmmcore.map

import io.typebrook.fmmcore.redux.CameraState

/**
 * Created by pham on 2017/10/16.
 * interface for redux subscriber
 */

interface MapControl {

    val cameraState: CameraState

    var cameraQueue: List<CameraState>
    var cameraStatePos: Int

    fun moveCamera(target: CameraState)
    fun animateCamera(target: CameraState)

    fun addTile(tileUrl: String?)
}