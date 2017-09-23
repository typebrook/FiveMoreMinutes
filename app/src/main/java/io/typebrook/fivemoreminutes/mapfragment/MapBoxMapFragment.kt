package io.typebrook.fivemoreminutes.mapfragment

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraPositionChange
import org.jetbrains.anko.UI
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.imageView
import org.jetbrains.anko.relativeLayout

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

    // region life cycle hard code
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        mapView.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }
    // endregion

    override fun onMapReady(map: MapboxMap) {

        val (lat, lon, zoom) = mainStore.state.cameraState
        map.moveCamera {
            CameraPosition.Builder()
                    .target(LatLng(lat, lon))
                    .zoom(zoom.toDouble())
                    .build()
        }

        map.setOnCameraMoveListener {
            val position = map.cameraPosition
            mainStore.dispatch(CameraPositionChange(
                    position.target.latitude, position.target.longitude, position.zoom.toFloat()))
        }
    }
}