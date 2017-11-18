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
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.fromStyle
import io.typebrook.fmmcore.redux.AddMap
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.RemoveMap
import io.typebrook.fmmcore.redux.UpdateCameraTarget
import org.jetbrains.anko.UI
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.imageView
import org.jetbrains.anko.relativeLayout
import java.net.URL


/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback, MapControl {

    private lateinit var map: GoogleMap

    override val cameraState: CameraState
        get() = map.cameraPosition.run { CameraState(target.latitude, target.longitude, zoom) }

    override var cameraQueue = listOf(mainStore.state.currentTarget)
    override var cameraStatePos: Int = 0

    override val selfStyles = listOf(
            "Google 街道" fromStyle GoogleMap.MAP_TYPE_NORMAL,
            "Google 衛星混合" fromStyle GoogleMap.MAP_TYPE_HYBRID
    )

    private var tileOverlay: TileOverlay? = null

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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        getMapAsync(this)
        mainStore.dispatch(AddMap(this))
    }

    override fun onDetach() {
        super.onDetach()
        mainStore.dispatch(RemoveMap(this))
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map

        moveCamera(cameraQueue.last())

        map.setOnCameraMoveListener {
            mainStore.dispatch(UpdateCameraTarget(this, cameraState))
        }

        map.setOnCameraIdleListener {
            if (!mainStore.state.cameraSave) return@setOnCameraIdleListener
            cameraStatePos += 1
            cameraQueue = cameraQueue.take(cameraStatePos) + cameraState
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }

        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
    }

    override fun moveCamera(target: CameraState) {
        val (lat, lon, zoom) = target
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))
    }

    override fun animateCamera(target: CameraState) {
        val (lat, lon, zoom) = target
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), 600, null)
    }

    override fun changeStyle(tileUrl: Any?) {
        map.mapType = tileUrl as Int
    }

    override fun changeWebTile(tileUrl: String?) {
        tileOverlay?.remove()
        if (tileUrl == null) {
            return
        }

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