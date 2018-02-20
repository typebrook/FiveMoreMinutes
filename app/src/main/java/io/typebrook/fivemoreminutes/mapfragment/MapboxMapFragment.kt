package io.typebrook.fivemoreminutes.mapfragment

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Fragment
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.view.FilePickerDialog
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.LatLngQuad
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapView.*
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.ImageSource
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import com.mapbox.services.android.location.LostLocationEngine
import com.mapbox.services.android.telemetry.location.LocationEngineListener
import com.mapbox.services.android.telemetry.location.LocationEnginePriority
import com.mapbox.services.android.telemetry.location.LostLocationEngine
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.localServer.MBTilesServer
import io.typebrook.fivemoreminutes.localServer.MBTilesSource
import io.typebrook.fivemoreminutes.localServer.MBTilesSourceError
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.utils.checkWriteExternal
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.map.fromStyle
import io.typebrook.fmmcore.projection.XYPair
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.File
import java.net.URL


/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class MapboxMapFragment : Fragment(), OnMapReadyCallback, MapControl, LocationEngineListener {

    private val mapView by lazy { MapView(activity, MapFragmentUtils.resolveArgs(activity, arguments)) }
    lateinit var map: MapboxMap

    private val locationPlugin by lazy { LocationLayerPlugin(mapView, map, locationEngine) }
    private val locationEngine by lazy {
        LostLocationEngine(ctx).apply {
            priority = LocationEnginePriority.HIGH_ACCURACY
            addLocationEngineListener(this@MapboxMapFragment)
        }
    }

    private lateinit var testButton: ImageView
    private lateinit var testButton2: ImageView
    private lateinit var progressIndicator: ProgressBar

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
            "Mapbox 衛星混合" fromStyle Style.SATELLITE_STREETS,
            "OpenMapTile" fromStyle "https://openmaptiles.github.io/osm-bright-gl-style/style-cdn.json"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(activity.applicationContext, resources.getString(R.string.token_mapbox))
    }

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {
        return UI {
            coordinatorLayout {
                addView(mapView)
                imageView {
                    backgroundResource = R.drawable.ic_cross_24dp
                }.lparams { gravity = Gravity.CENTER }
                progressIndicator = progressBar().lparams { gravity = Gravity.CENTER }
                testButton = imageView {
                    backgroundResource = R.drawable.mapbutton_background
                }.lparams { topMargin = 180 }
                testButton2 = imageView {
                    backgroundResource = R.drawable.mapbutton_background
                }.lparams { topMargin = 300 }
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

        map.addOnMapClickListener { mainStore dispatch FocusMap(this) }

        map.addOnCameraMoveListener {
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        map.addOnCameraIdleListener {
            if (!mainStore.state.cameraSave) return@addOnCameraIdleListener
            cameraStatePos += 1
            cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        mapView.addOnMapChangedListener { change ->
            Log.d("mapChange", change.toString())
            when (change) {
                WILL_START_RENDERING_FRAME, WILL_START_RENDERING_MAP ->
                    progressIndicator.visibility = VISIBLE

                DID_FINISH_RENDERING_FRAME_FULLY_RENDERED, DID_FINISH_RENDERING_MAP_FULLY_RENDERED ->
                    progressIndicator.visibility = INVISIBLE
            }
        }

        testButton2.onClick {
            //            toast("Yahoo~")
//            val intent = PlaceAutocomplete.IntentBuilder()
//                    .accessToken(Mapbox.getAccessToken())
//                    .placeOptions(PlaceOptions.builder()
//                            .backgroundColor(Color.parseColor("#EEEEEE"))
//                            .limit(10)
//                            .build(PlaceOptions.MODE_CARDS))
//                    .build(ctx as Activity)
//            startActivityForResult(intent, 1)
        }

        testButton.onClick {
            if (!checkWriteExternal(ctx)) return@onClick

            if (MBTilesServer.isRunning) {
                MBTilesServer.stop()
                toast("stop")
                return@onClick
            }

            FilePickerDialog(ctx).apply {
                setTitle("Select Mbtiles")
                properties.selection_mode = DialogConfigs.SINGLE_MODE
                properties.selection_type = DialogConfigs.FILE_SELECT
                properties.extensions = arrayOf("mbtiles")
                properties.root = File(Environment.getExternalStorageDirectory().path)
                properties.offset = File(properties.root.path + "/Download")
                properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
                setDialogSelectionListener { files ->
                    val ms = try {
                        MBTilesSource(files[0], "openmaptiles").apply { activate() }
                    } catch (e: MBTilesSourceError.CouldNotReadFileError) {
                        toast("Could Not ReadFile")
                        return@setDialogSelectionListener
                    }
                    if (!ms.isVector) {
                        map.addSource(RasterSource(ms.id, TileSet(null, ms.url), 126))
                        map.removeLayer(ID_WEBLAYER_BASE)
                        val rasterLayer = RasterLayer(ID_WEBLAYER_BASE, ms.id)
                        map.addLayer(rasterLayer)
                    } else {
                        map.setStyleUrl("asset://mbtiles.json")
                    }
                }
                show()
            }

            //            val list = listOf(
//                    "Compass" to LocationLayerMode.COMPASS,
//                    "Tracking" to LocationLayerMode.TRACKING,
//                    "Navigation" to LocationLayerMode.NAVIGATION,
//                    "None" to LocationLayerMode.NONE)
//            selector("Location mode", list.map { it.first }) { _, index ->
//                if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
//                    locationPlugin.setLocationLayerEnabled(list[index].second)
//                }
//            }
        }

        map.uiSettings.compassGravity = Gravity.START
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
        map.setStyleUrl(newStyle as String)
    }

    override fun setWebTile(tile: Tile.WebTile?) {
        map.removeLayer(ID_WEBLAYER_BASE)
        map.removeSource(ID_WEBSOURCE_BASE)
        if (tile == null) return

        if (map.styleUrl != "asset://Single_Web_Tile.json") {
            map.setStyle("asset://Single_Web_Tile.json") {
                setWebTile(tile)
            }
            return
        }

        val webMapSource = RasterSource(ID_WEBSOURCE_BASE, TileSet(null, tile.url), tile.size)
        map.addSource(webMapSource)

        // Add the web map source to the map.
        val webMapLayer = RasterLayer(ID_WEBLAYER_BASE, ID_WEBSOURCE_BASE)
        map.addLayer(webMapLayer)
    }

    override fun addWebTile(tile: Tile.WebTile) {
        val webMapSource = RasterSource(tile.url, TileSet(null, tile.url), tile.size)
        map.addSource(webMapSource)

        // Add the web map source to the map.
        val webMapLayer = RasterLayer(tile.name, tile.url)
        map.addLayer(webMapLayer)
    }

    override fun addWebImage(tile: Tile.WebImage) {
        val (topLeft, bottomRight) = tile.bound ?: return
        val bound = LatLngQuad(
                LatLng(topLeft.second, topLeft.first),
                LatLng(topLeft.second, bottomRight.first),
                LatLng(bottomRight.second, bottomRight.first),
                LatLng(bottomRight.second, topLeft.first)
        )
        val webImageSource = ImageSource(tile.url, bound, URL(tile.url))
        map.addSource(webImageSource)

        // Add the web map source to the map.
        val webImageLayer = RasterLayer(tile.name, tile.url)
        map.addLayer(webImageLayer)
    }


    // region User Location
    override fun enableLocation() {
        locationEngine.activate()
    }

    override fun disableLocation() {
        locationEngine.deactivate()
        mainStore dispatch DidSwitchLocation(this, false)
        if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
            locationPlugin.setLocationLayerEnabled(LocationLayerMode.NONE)
        }
    }

    override fun onLocationChanged(location: Location?) {}
    override fun onConnected() {
        if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
            mainStore dispatch DidSwitchLocation(this, true)

            val lastLocation = locationEngine.lastLocation
            val zoom = if (cameraState.zoom < 16) 16f else cameraState.zoom
            lastLocation?.run { animateCamera(CameraState(latitude, longitude, zoom), 800) }

            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING)
        }
    }

    // endregion
    companion object {
        val ID_WEBSOURCE_BASE = "web-map-source"
        val ID_WEBLAYER_BASE = "web-map-layer"

        val ZOOMOFFSET = 1
    }
}