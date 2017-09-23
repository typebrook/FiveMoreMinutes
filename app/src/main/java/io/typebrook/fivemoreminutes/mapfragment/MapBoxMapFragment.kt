package io.typebrook.fivemoreminutes

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.maps.MapFragment
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.typebrook.fivemoreminutes.redux.CameraPositionChange
import org.jetbrains.anko.*

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class MapBoxMapFragment : Fragment(), OnMapReadyCallback {

    private val mapView by lazy { MapView(activity, MapFragmentUtils.resolveArgs(activity, arguments)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(activity.applicationContext, resources.getString(R.string.token_mapbox))
    }

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {

        return UI {
            relativeLayout {
                addView(mapView)
                imageView {
                    background = resources.getDrawable(R.drawable.ic_cross_24dp)
                }.lparams { centerInParent() }
            }
        }.view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: MapboxMap) {

//        val lastCameraPosition = mainStore.state.lastCameraPosition
//        map.animateCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition))
//
        map.setOnCameraMoveListener {
            val position = map.cameraPosition
            mainStore.dispatch(CameraPositionChange(CameraPosition(
                    LatLng(position.target.latitude, position.target.longitude), position.zoom.toFloat(), 0f, 0f)))
            Log.d("dispatch", "postion")
        }

        activity.toast("mapReady")

        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }
    }
}