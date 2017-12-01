package io.typebrook.fivemoreminutes

import android.app.Activity
import android.os.Bundle
import io.typebrook.fivemoreminutes.mapfragment.DualMapFragment
import io.typebrook.fivemoreminutes.mapfragment.GoogleMapFragment
import io.typebrook.fivemoreminutes.mapfragment.MapboxMapFragment
import io.typebrook.fivemoreminutes.ui.ActivityUI
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.redux.TargetBackward
import org.jetbrains.anko.contentView
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.setContentView
import tw.geothings.rekotlin.StoreSubscriber

class MainActivity : Activity(), StoreSubscriber<Display> {

    private lateinit var ui: ActivityUI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityUI().apply { setContentView(this@MainActivity) }

        mainStore.subscribe(this) { subscription -> subscription.select { it.display }.skipRepeats() }
    }

    override fun onBackPressed() {
        mainStore dispatch TargetBackward()
        contentView?.let {
            snackbar(it, mainStore.state.mapState.run { maps[currentMapNum].mapControl.cameraStatePos.toString() }, "leave", { super.onBackPressed() })
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
                        .replace(mapContainer.id, DualMapFragment().apply { insertMap(MapboxMapFragment(), GoogleMapFragment()) })
                        .commit()
            }
        }
    }
}
