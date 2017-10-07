package io.typebrook.fivemoreminutes.mapfragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.mapbox.mapboxsdk.camera.CameraPosition
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraPositionChange
import io.typebrook.fivemoreminutes.redux.CameraPositionSave
import io.typebrook.fivemoreminutes.redux.CameraState
import org.jetbrains.anko.UI
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.imageView
import org.jetbrains.anko.relativeLayout
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback, StoreSubscriber<Int> {

    private lateinit var map: GoogleMap

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
        this.map = map
        mainStore.subscribe(this) { subscription ->
            subscription.select { it.cameraStatePos }.only { oldState, newState -> newState < oldState }
        }

        val (lat, lon, zoom) = mainStore.state.currentTarget
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))

        map.setOnCameraMoveListener {
            val position = map.cameraPosition
            mainStore.dispatch(CameraPositionChange(CameraState(
                    position.target.latitude,
                    position.target.longitude,
                    position.zoom)))
        }

        map.setOnCameraIdleListener {
            val position = map.cameraPosition
            Log.d("states", "====>on Idle")
            mainStore.dispatch(CameraPositionSave(CameraState(
                    position.target.latitude,
                    position.target.longitude,
                    position.zoom)))
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }
    }

    override fun newState(state: Int) {
        val (lat, lon, zoom) = mainStore.state.previousCameraStates[state]
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), 600, null)
    }
}