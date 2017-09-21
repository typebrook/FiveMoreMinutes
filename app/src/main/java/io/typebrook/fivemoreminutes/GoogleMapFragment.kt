package io.typebrook.fivemoreminutes

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import io.typebrook.fivemoreminutes.redux.CameraPositionChange

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback {

    init {
        getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {

        val lastCameraPosition = mainStore.state.lastCameraPosition
        map.moveCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition))

        map.setOnCameraMoveListener {
            mainStore.dispatch(CameraPositionChange(map.cameraPosition))
        }

        map.uiSettings.apply{
            isZoomControlsEnabled = true
        }
    }
}