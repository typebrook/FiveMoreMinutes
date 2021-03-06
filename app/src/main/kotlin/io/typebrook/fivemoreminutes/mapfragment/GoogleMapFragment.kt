package io.typebrook.fivemoreminutes.mapfragment

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.map.fromStyle
import io.typebrook.fmmcore.realm.projection.XYPair
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import java.net.URL


/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback, MapControl {

    private lateinit var map: GoogleMap

    override val cameraState: CameraState
        get() = map.cameraPosition.run { CameraState(target.latitude, target.longitude, zoom) }
    override val screenBound: Pair<XYPair, XYPair>
        get() = map.projection.visibleRegion.latLngBounds.run {
            (northeast.latitude to northeast.longitude) to (southwest.latitude to southwest.longitude)
        }

    override var cameraQueue = listOf(mainStore.state.currentCamera)
    override var cameraStatePos: Int = 0

    override val styles = listOf(
            "Google 衛星混合" fromStyle GoogleMap.MAP_TYPE_HYBRID,
            "Google 街道" fromStyle GoogleMap.MAP_TYPE_NORMAL,
            "Google 地形" fromStyle GoogleMap.MAP_TYPE_TERRAIN
    )

    private var baseTileOverlay: TileOverlay? = null
    private var tileOverlays: List<TileOverlay> = listOf()

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {
        getMapAsync(this)
        return UI {
            relativeLayout {
                addView(super.onCreateView(p0, p1, p2))
                imageView {
                    backgroundResource = R.drawable.ic_cross_24dp
                }.lparams { centerInParent() }
            }
        }.view
    }

    override fun onDetach() {
        super.onDetach()
        mainStore.dispatch(RemoveMap(this))
    }

    override fun onMapReady(map: GoogleMap) {
        if (this::map.isInitialized) return

        this.map = map
        mainStore dispatch AddMap(this)
        moveCamera(cameraQueue.last())

        map.run {
            setOnMapClickListener {
                mainStore dispatch FocusMap(this@GoogleMapFragment)
                mainStore dispatch SwitchComponentVisibility()
            }

            setOnCameraMoveStartedListener {
                mainStore dispatch UpdateCurrentTarget(this@GoogleMapFragment, cameraState)
            }

            setOnCameraMoveListener {
                mainStore dispatch UpdateCurrentTarget(this@GoogleMapFragment, cameraState)
            }

            setOnCameraIdleListener {
                if (!mainStore.state.cameraSave) return@setOnCameraIdleListener
                cameraStatePos += 1
                cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
                mainStore dispatch UpdateCurrentTarget(this@GoogleMapFragment, cameraState)
            }
        }
    }

    override fun moveCamera(target: CameraState) {
        val (lat, lon, zoom) = target
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))
        mainStore dispatch UpdateCurrentTarget(this, target)
    }

    override fun animateCamera(target: CameraState, duration: Int) {
        val (lat, lon, zoom) = target
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), duration, null)
    }

    override fun animateToBound(ne: XYPair, sw: XYPair, duration: Int) {
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                LatLngBounds(LatLng(ne.first, ne.second), LatLng(sw.first, sw.second)), 30), duration, null)
    }

    override fun zoomBy(value: Float) {
        map.animateCamera(CameraUpdateFactory.zoomBy(value))
    }

    override fun setStyle(style: Tile.PrivateStyle?) {
        baseTileOverlay?.remove()
        val mapType = style?.value.takeIf { styles.contains(style) } ?: styles[0].value
        map.mapType = mapType as Int
    }

    override fun setWebTile(tile: Tile.WebTile?) {
        baseTileOverlay?.remove()
        map.mapType = GoogleMap.MAP_TYPE_NONE
        if (tile == null) return

        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, z: Int): URL {
                val urlString = tile.url
                        .replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                        .replace("{z}", z.toString())
                return URL(urlString)
            }
        }

        baseTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    }

    override fun addWebTile(tile: Tile.WebTile) {

        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, z: Int): URL {
                val urlString = tile.url
                        .replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                        .replace("{z}", z.toString())
                return URL(urlString)
            }
        }

        tileOverlays += map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    }

    override fun enableLocation() {
        if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
            fusedLocationClient.lastLocation.addOnSuccessListener(activity) { lastLocation ->
                mainStore dispatch DidSwitchLocation(this, true)
                lastLocation?.apply {
                    val zoom = if (cameraState.zoom < 16) 16f else cameraState.zoom
                    animateCamera(CameraState(latitude, longitude, zoom), 1000)
                }
            }
        }
    }

    override fun disableLocation() {
        if (activity.checkPermission(ACCESS_FINE_LOCATION, 0, 0) == PERMISSION_GRANTED) {
            map.isMyLocationEnabled = false
            mainStore dispatch DidSwitchLocation(this, false)
        }
    }
}