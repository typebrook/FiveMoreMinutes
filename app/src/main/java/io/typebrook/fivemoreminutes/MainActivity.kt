package io.typebrook.fivemoreminutes

import android.app.Activity
import android.os.Bundle
import io.typebrook.fivemoreminutes.mapfragment.GoogleMapFragment
import io.typebrook.fivemoreminutes.redux.CameraPositionBackward
import io.typebrook.fivemoreminutes.ui.ActivityUI
import org.jetbrains.anko.contentView
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.setContentView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityUI().setContentView(this)

        fragmentManager.beginTransaction()
                .add(ActivityUI.ID_MAP_CONTAINER, GoogleMapFragment())
                .commit()
    }

    override fun onBackPressed() {
        mainStore.dispatch(CameraPositionBackward())
        contentView?.let {
            snackbar(it, mainStore.state.cameraStatePos.toString(), "leave", { super.onBackPressed() })
        }
    }
}
