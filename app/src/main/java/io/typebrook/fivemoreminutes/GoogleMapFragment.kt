package io.typebrook.fivemoreminutes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*
import org.jetbrains.anko.toast

/**
 * Created by pham on 2017/9/19.
 */
class GoogleMapFragment : MapFragment(), OnMapReadyCallback {

    init {
        getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        map.moveCamera(CameraUpdateFactory.newCameraPosition(postion))

        map.setOnCameraMoveListener {
           postion = map.cameraPosition
        }
    }
}