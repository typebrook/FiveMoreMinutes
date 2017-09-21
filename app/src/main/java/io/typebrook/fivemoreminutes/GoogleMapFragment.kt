package io.typebrook.fivemoreminutes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import io.typebrook.fivemoreminutes.redux.CameraPositionChange
import org.jetbrains.anko.UI
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.imageView
import org.jetbrains.anko.relativeLayout

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback {

    init {
        getMapAsync(this)
    }

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {
        return UI {
            relativeLayout {
                addView(super.onCreateView(p0, p1, p2))
                imageView {
                    background = resources.getDrawable(R.drawable.ic_cross_24dp)
                }.lparams { centerInParent() }
            }
        }.view
    }

    override fun onMapReady(map: GoogleMap) {

        val lastCameraPosition = mainStore.state.lastCameraPosition
        map.moveCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition))

        map.setOnCameraMoveListener {
            mainStore.dispatch(CameraPositionChange(map.cameraPosition))
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }
    }
}