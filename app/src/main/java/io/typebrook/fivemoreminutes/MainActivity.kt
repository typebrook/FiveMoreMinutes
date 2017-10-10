package io.typebrook.fivemoreminutes

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityUI().setContentView(this)

        mainStore.subscribe(this) { subscription -> subscription.select { it.display }.skipRepeats() }
    }

    override fun onBackPressed() {
        mainStore.dispatch(CameraPositionBackward())
        contentView?.let {
            snackbar(it, mainStore.state.cameraStatePos.toString(), "leave", { super.onBackPressed() })
        }
    }

    override fun newState(state: Display) {
//        val container = contentView?.find<FrameLayout>(ActivityUI.ID_MAP_CONTAINER)
//        container?.removeAllViews()

        when (state) {
            Display.Google -> {
                fragmentManager.beginTransaction()
                        .replace(ActivityUI.ID_MAP_CONTAINER, GoogleMapFragment())
                        .commit()
            }
            Display.MapBox -> {
                fragmentManager.beginTransaction()
                        .replace(ActivityUI.ID_MAP_CONTAINER, MapboxMapFragment())
                        .commit()
            }
            Display.Dual -> {
//                container?.addView(UI {
//                    verticalLayout {
//                        frameLayout { id = 1001 }.lparams { weight = 1f }
//                        frameLayout { id = 1002 }.lparams { weight = 1f }
//                    }
//                }.view)
//
//                fragmentManager.beginTransaction()
//                        .replace(1001, GoogleMapFragment(), "map_fragment_1")
//                        .replace(1002, MapboxMapFragment(), "map_fragment_2")
//                        .commit()
            }
        }
    }
}
