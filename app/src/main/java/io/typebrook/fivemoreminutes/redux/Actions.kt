package io.typebrook.fivemoreminutes.redux

import com.google.android.gms.maps.model.CameraPosition
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

data class CameraPositionChange(val lat: Double, val lon: Double, val zoom: Float) : Action
data class CameraPositionSave(val lat: Double, val lon: Double, val zoom: Float) : Action
class CameraPositionReturn : Action