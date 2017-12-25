package io.typebrook.fivemoreminutes.mapfragment

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Fragment
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationSource
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.map.fromStyle
import io.typebrook.fmmcore.projection.XYPair
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class MapboxMapFragment : Fragment(), OnMapReadyCallback, MapControl {

    private val mapView by lazy { MapView(activity, MapFragmentUtils.resolveArgs(activity, arguments)) }
    private lateinit var map: MapboxMap
    private lateinit var testButton: ImageView

    override val cameraState: CameraState
        get() = map.cameraPosition.run { CameraState(target.latitude, target.longitude, zoom.toFloat() + ZOOMOFFSET) }
    override val screenBound: Pair<XYPair, XYPair>
        get() = map.projection.visibleRegion.latLngBounds.run {
            (latNorth to lonEast) to (latSouth to lonWest)
        }

    override var cameraQueue = listOf(mainStore.state.currentCamera)
    override var cameraStatePos: Int = 0

    override val styles = listOf(
            "Taiwan Topo on Web" fromStyle "mapbox://styles/typebrook/cjb2gaiwv59ay2so0ofmzfzji",
            "Taiwan Topo" fromStyle "asset://Taiwan_Topo.json",
            "Mapbox 戶外" fromStyle Style.OUTDOORS,
            "Mapbox 街道" fromStyle Style.MAPBOX_STREETS,
            "Mapbox 衛星混合" fromStyle Style.SATELLITE_STREETS
    )

    override val locating get() = map.isMyLocationEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(activity.applicationContext, resources.getString(R.string.token_mapbox))
    }

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {
        return UI {
            relativeLayout {
                addView(mapView)
                imageView {
                    backgroundResource = R.drawable.ic_cross_24dp
                }.lparams { centerInParent() }
                testButton = imageView {
                    backgroundResource = R.drawable.mapbutton_background
                }
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

    override fun onDetach() {
        super.onDetach()
        mainStore.dispatch(RemoveMap(this))
    }

    // endregion

    override fun onMapReady(map: MapboxMap) {
        this.map = map
        mainStore dispatch AddMap(this)
        animateCamera(cameraQueue.last(), 10)
        map.layers.forEach { it.setProperties(textField("{name_zh}")) } // may set on original style

        map.setOnMapClickListener { mainStore dispatch FocusMap(this) }

        map.setOnCameraMoveListener {
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        map.setOnCameraIdleListener {
            if (!mainStore.state.cameraSave) return@setOnCameraIdleListener
            cameraStatePos += 1
            cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        mapView.addOnMapChangedListener { change ->
            Log.d("mapChange", change.toString())
            when (change) {
                MapView.DID_FINISH_RENDERING_MAP_FULLY_RENDERED -> toast("Finish Loading")
                MapView.DID_FINISH_LOADING_MAP ->
                    map.layers.forEach { it.setProperties(textField("{name}")) }
            }
        }

        testButton.onClick {
            map.layers.forEach { it.setProperties(textField("{name_en}")) }
        }
    }

    override fun moveCamera(target: CameraState) {
        val (lat, lon, zoom) = target
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom.toDouble() - ZOOMOFFSET))
        mainStore dispatch UpdateCurrentTarget(this, target)
    }

    override fun animateCamera(target: CameraState, duration: Int) {
        val (lat, lon, zoom) = target
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom.toDouble() - ZOOMOFFSET), duration)
    }

    override fun animateToBound(ne: XYPair, sw: XYPair, duration: Int) {
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.from(ne.first, ne.second, sw.first, sw.second), 30), duration)
    }

    override fun zoomBy(value: Float) {
        map.animateCamera(CameraUpdateFactory.zoomBy(value.toDouble()))
    }

    override fun setStyle(style: Tile.PrivateStyle?) {
        setWebTile(null)
        val newStyle = style?.value?.takeIf { styles.contains(style) } ?: styles[0].value
        map.setStyle(newStyle as String)
    }

    override fun setWebTile(tile: Tile.WebTile?) {
        map.removeLayer(ID_WEBLAYER_BASE)
        map.removeSource(ID_WEBSOURCE_BASE)
        if (tile == null) return

        if (map.styleUrl != "asset://None.json") {
            map.setStyle("asset://None.json") {
                setWebTile(tile)
            }
            return
        }

        val webMapSource = RasterSource(ID_WEBSOURCE_BASE, TileSet(null, tile.url))
        map.addSource(webMapSource)

        // Add the web map source to the map.
        val webMapLayer = RasterLayer(ID_WEBLAYER_BASE, ID_WEBSOURCE_BASE)
        map.addLayer(webMapLayer)
    }

    override fun addWebTile(tile: Tile.WebTile) {
        val webMapSource = RasterSource(tile.url, TileSet(null, tile.url))
        map.addSource(webMapSource)

        // Add the web map source to the map.
        val webMapLayer = RasterLayer(tile.name, tile.url)
        map.addLayer(webMapLayer)
    }

    override fun enableLocation() {
        if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            val lastLocation = LocationSource(activity).lastLocation
            lastLocation?.apply {
                val zoom = if (cameraState.zoom < 16) 16f else cameraState.zoom
                animateCamera(CameraState(latitude, longitude, zoom), 1000)
            }
        }
    }

    override fun disableLocation() {
        map.isMyLocationEnabled = false
    }

    //endregion

    companion object {
        val ID_WEBSOURCE_BASE = "web-map-source"
        val ID_WEBLAYER_BASE = "web-map-layer"

        val ZOOMOFFSET = 1
    }
}