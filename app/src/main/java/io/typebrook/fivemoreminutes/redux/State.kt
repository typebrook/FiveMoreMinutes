package io.typebrook.fivemoreminutes.redux

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import tw.geothings.rekotlin.StateType

/**
 * Created by pham on 2017/9/20.
 */
data class State(
        val lastCameraPosition: CameraPosition = CameraPosition(LatLng(24.782347, 121.035044), 15f, 0f, 0f)
) : StateType