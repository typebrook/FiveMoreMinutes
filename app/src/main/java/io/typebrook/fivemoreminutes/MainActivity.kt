package io.typebrook.fivemoreminutes

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.widget.FrameLayout
import io.typebrook.fivemoreminutes.mapfragment.Display
import io.typebrook.fivemoreminutes.mapfragment.DualMapFragment
import io.typebrook.fivemoreminutes.mapfragment.GoogleMapFragment
import io.typebrook.fivemoreminutes.mapfragment.MapboxMapFragment
import io.typebrook.fivemoreminutes.redux.CameraPositionBackward
import io.typebrook.fivemoreminutes.ui.ActivityUI
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import tw.geothings.rekotlin.StoreSubscriber

class MainActivity : Activity(), StoreSubscriber<Display> {

    private lateinit var ui: ActivityUI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityUI().apply { setContentView(this@MainActivity) }

        mainStore.subscribe(this) { subscription -> subscription.select { it.display }.skipRepeats() }
    }

    override fun onBackPressed() {
        mainStore.dispatch(CameraPositionBackward())
        contentView?.let {
            snackbar(it, mainStore.state.cameraStatePos.toString(), "leave", { super.onBackPressed() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainStore.unsubscribe(this)
    }

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
                        .replace(mapContainer.id, DualMapFragment(GoogleMapFragment(), MapboxMapFragment()))
                        .commit()
            }
        }
    }
}
