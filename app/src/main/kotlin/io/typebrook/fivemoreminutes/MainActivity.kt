package io.typebrook.fivemoreminutes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mapbox.services.android.telemetry.permissions.PermissionsListener
import io.typebrook.fivemoreminutes.mapfragment.DualMapFragment
import io.typebrook.fivemoreminutes.mapfragment.GoogleMapFragment
import io.typebrook.fivemoreminutes.mapfragment.MapboxMapFragment
import io.typebrook.fivemoreminutes.ui.ActivityUI
import io.typebrook.fivemoreminutes.ui.ActivityUI.Companion.id_map_container
import io.typebrook.fivemoreminutes.utils.REQUEST_CHECK_SETTINGS
import io.typebrook.fivemoreminutes.utils.intentHandler
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.redux.BackPressed
import io.typebrook.fmmcore.redux.EnableLocation
import io.typebrook.fmmcore.redux.SetContext
import io.typebrook.fmmcore.redux.TargetBackward
import org.jetbrains.anko.setContentView
import org.jetbrains.anko.toast
import tw.geothings.rekotlin.StoreSubscriber

class MainActivity : Activity(), StoreSubscriber<Display>, PermissionsListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityUI().apply { setContentView(this@MainActivity) }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.parseColor("#50000000")
        }

        mainStore dispatch SetContext(this)
        mainStore.subscribe(this) { subscription -> subscription.select { it.display }.skipRepeats() }

        intent?.let(intentHandler)
    }

    override fun onBackPressed() {
        mainStore dispatch BackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainStore.unsubscribe(this)
    }

    // newState for mapView layout
    override fun newState(state: Display) {

        when (state) {
            Display.Google -> {
                fragmentManager.beginTransaction()
                        .replace(id_map_container, GoogleMapFragment())
                        .commit()
            }
            Display.MapBox -> {
                fragmentManager.beginTransaction()
                        .replace(id_map_container, MapboxMapFragment())
                        .commit()
            }
            Display.Dual -> {
                fragmentManager.beginTransaction()
                        .replace(id_map_container, DualMapFragment().apply { insertMap(MapboxMapFragment(), GoogleMapFragment()) })
                        .commit()
            }
        }
    }

    // region request permission in APP
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> if (resultCode == RESULT_OK) {
                mainStore dispatch EnableLocation()
            }
        }
    }
}
