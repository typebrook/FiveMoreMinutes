package io.typebrook.fivemoreminutes

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import com.mapbox.services.android.telemetry.permissions.PermissionsListener
import io.typebrook.fivemoreminutes.mapfragment.DualMapFragment
import io.typebrook.fivemoreminutes.mapfragment.GoogleMapFragment
import io.typebrook.fivemoreminutes.mapfragment.MapboxMapFragment
import io.typebrook.fivemoreminutes.ui.ActivityUI
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.redux.EnableLocation
import io.typebrook.fmmcore.redux.TargetBackward
import org.jetbrains.anko.contentView
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.setContentView
import org.jetbrains.anko.toast
import tw.geothings.rekotlin.StoreSubscriber

class MainActivity : Activity(), StoreSubscriber<Display>, PermissionsListener {

    private lateinit var ui: ActivityUI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityUI().apply { setContentView(this@MainActivity) }

        mainStore.subscribe(this) { subscription -> subscription.select { it.display }.skipRepeats() }
    }

    override fun onBackPressed() {
        mainStore dispatch TargetBackward()
        contentView?.let {
            snackbar(it, mainStore.state.currentMap.mapControl.cameraStatePos.toString(), "leave", { super.onBackPressed() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainStore.unsubscribe(this)
    }

    // newState for mapView layout
    override fun newState(state: Display) {
        val mapContainer = ui.mapContainer

        when (state) {
            Display.Google -> {
                fragmentManager.beginTransaction()
                        .replace(mapContainer.id, GoogleMapFragment())
                        .commit()
            }
            Display.MapBox -> {
                fragmentManager.beginTransaction()
                        .replace(mapContainer.id, MapboxMapFragment())
                        .commit()
            }
            Display.Dual -> {
                fragmentManager.beginTransaction()
                        .replace(mapContainer.id, DualMapFragment().apply { insertMap(MapboxMapFragment(), GoogleMapFragment()) })
                        .commit()
            }
        }
    }

    // region locate myself
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mainStore dispatch EnableLocation()
        } else {
            toast("not granted")
        }
    }

    override fun onPermissionResult(granted: Boolean) {}

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {}
    // endregion
}
