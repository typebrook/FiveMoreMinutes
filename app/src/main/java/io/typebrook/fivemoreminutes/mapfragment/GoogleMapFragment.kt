package io.typebrook.fivemoreminutes.mapfragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.UrlTileProvider
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.fromStyle
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

    override var cameraQueue = listOf(mainStore.state.currentCamera)
    override var cameraStatePos: Int = 0

    override val styles = listOf(
            "Google 衛星混合" fromStyle GoogleMap.MAP_TYPE_HYBRID,
            "Google 街道" fromStyle GoogleMap.MAP_TYPE_NORMAL,
            "Google 地形" fromStyle GoogleMap.MAP_TYPE_TERRAIN
    )

    private var tileOverlay: TileOverlay? = null

    override fun onCreateView(p0: LayoutInflater?, p1: ViewGroup?, p2: Bundle?): View {
        getMapAsync(this)
        return UI {
            relativeLayout {
                addView(super.onCreateView(p0, p1, p2))
                imageView {
                    background = context.getDrawable(R.drawable.ic_cross_24dp)
                }.lparams { centerInParent() }
            }
        }.view
    }

    override fun onDetach() {
        super.onDetach()
        mainStore.dispatch(RemoveMap(this))
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        mainStore.dispatch(AddMap(this))
        moveCamera(cameraQueue.last())

        map.setOnMapClickListener { mainStore dispatch FocusMap(this) }

        map.setOnCameraMoveStartedListener {
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        map.setOnCameraMoveListener {
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
        }

        map.setOnCameraIdleListener {
            if (!mainStore.state.cameraSave) return@setOnCameraIdleListener
            cameraStatePos += 1
            cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
            mainStore dispatch UpdateCurrentTarget(this, cameraState)
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

    override fun zoomBy(value: Float) {
        map.animateCamera(CameraUpdateFactory.zoomBy(value))
    }

    override fun changeStyle(tileUrl: Any?) {
        val mapType = tileUrl?.takeIf { styles.map { it.value }.contains(it) } ?: styles[0].value
        map.mapType = mapType as Int
    }

    override fun changeWebTile(tileUrl: String?) {
        tileOverlay?.remove()
        if (tileUrl == null) return

        val tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, z: Int): URL {
                val urlString = tileUrl
                        .replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                        .replace("{z}", z.toString())
                return URL(urlString)
            }
        }

        tileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
    }
}