package io.typebrook.fivemoreminutes.mapfragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.mapbox.mapboxsdk.camera.CameraPosition
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraPositionChange
import io.typebrook.fivemoreminutes.redux.CameraPositionSave
import io.typebrook.fivemoreminutes.redux.CameraState
import org.jetbrains.anko.*
import tw.geothings.rekotlin.StoreSubscriber
import java.net.URL
import java.lang.reflect.AccessibleObject.setAccessible



/**
 * Created by pham on 2017/9/19.
 * this fragment defines Google Map interaction with user
 */

class GoogleMapFragment : MapFragment(), OnMapReadyCallback, MapControl {

    private lateinit var map: GoogleMap

    private val cameraListener = CameraListener(this)
    private val tileListener = TileListener(this)
    private var tileOverlay: TileOverlay? = null

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

    override fun onDetach() {
        toast("detach ${this::class.java.simpleName}")
        super.onDetach()
        mainStore.unsubscribe(cameraListener)
        mainStore.unsubscribe(tileListener)
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        mainStore.subscribe(cameraListener) { subscription ->
            subscription.select { it.cameraStatePos }
                    .skipRepeats()
                    .only { oldState, newState -> newState < oldState }
        }

        mainStore.subscribe(tileListener) { subscription ->
            subscription.select { it.tileUrl }.skipRepeats()
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
            mainStore.dispatch(CameraPositionSave(CameraState(
                    position.target.latitude,
                    position.target.longitude,
                    position.zoom)))
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = true
        }
    }


    override fun animateCamera(lat: Double, lon: Double, zoom: Float) {
        toast("animate!")
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom), 600, null)
    }

    override fun addTile(tileUrl: String?) {
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