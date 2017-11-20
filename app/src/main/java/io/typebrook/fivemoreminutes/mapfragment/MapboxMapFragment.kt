package io.typebrook.fivemoreminutes.mapfragment

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.fromStyle
import io.typebrook.fmmcore.redux.AddMap
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.RemoveMap
import io.typebrook.fmmcore.redux.UpdateCurrentTarget
import org.jetbrains.anko.UI
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.imageView
import org.jetbrains.anko.relativeLayout

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class MapboxMapFragment : Fragment(), OnMapReadyCallback, MapControl {

    private val mapView by lazy { MapView(activity, MapFragmentUtils.resolveArgs(activity, arguments)) }
    private lateinit var map: MapboxMap

    override val cameraState: CameraState
        get() = map.cameraPosition.run { CameraState(target.latitude, target.longitude, zoom.toFloat() + ZOOMOFFSET) }

    override var cameraQueue = listOf(mainStore.state.currentCamera)
    override var cameraStatePos: Int = 0

    override val styles = listOf(
            "Mapbox 戶外" fromStyle Style.OUTDOORS,
            "Mapbox 衛星混合" fromStyle Style.SATELLITE_STREETS
    )

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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mainStore.dispatch(AddMap(this))
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

    override fun onDetach() {
        super.onDetach()
        mainStore.dispatch(RemoveMap(this))
    }

    // endregion

    override fun onMapReady(map: MapboxMap) {
        this.map = map

        moveCamera(cameraQueue.last())

        map.setOnCameraMoveListener {
            mainStore.dispatch(UpdateCurrentTarget(this, cameraState))
        }

        map.setOnCameraIdleListener {
            if (!mainStore.state.cameraSave) return@setOnCameraIdleListener
            cameraStatePos += 1
            cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
        }
    }

    override fun moveCamera(target: CameraState) {
        val (lat, lon, zoom) = target
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom.toDouble() - ZOOMOFFSET))
    }

    override fun animateCamera(target: CameraState, duration: Int) {
        val (lat, lon, zoom) = target
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom.toDouble() - ZOOMOFFSET), duration)
    }

    override fun zoomBy(value: Float) {
        map.animateCamera(CameraUpdateFactory.zoomBy(value.toDouble()))
    }

    override fun changeStyle(tileUrl: Any?) {
        map.setStyle(tileUrl as String)
    }

    override fun changeWebTile(tileUrl: String?) {
        map.removeLayer(ID_WEBLAYER)
        map.removeSource(ID_WEBSOURCE)
        if (tileUrl == null) {
            return
        }

        val webMapSource = RasterSource(ID_WEBSOURCE, TileSet(null, tileUrl), 256)
        map.addSource(webMapSource)

        // Add the web map source to the map.
        val webMapLayer = RasterLayer(ID_WEBLAYER, ID_WEBSOURCE)
        map.addLayer(webMapLayer)
    }

    companion object {
        val ID_WEBSOURCE = "web-map-source"
        val ID_WEBLAYER = "web-map-layer"

        val ZOOMOFFSET = 1
    }
}