package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.view.Gravity
import android.widget.TextView
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraState
import org.jetbrains.anko.*
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.osgeo.proj4j.CoordinateTransform
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */
class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    private var coordinate: TextView? = null
    private var projTransform: CoordinateTransform? = null

    companion object {
        val ID_CONTAINER = 1000
    }

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {

        relativeLayout {

            verticalLayout {
                id = ID_CONTAINER
            }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = Color.parseColor("#80FFFFFF")
            }.lparams(wrapContent) {
                alignParentBottom()
                centerHorizontally()
                bottomMargin = dip(10)
            }

            floatingActionButton{
                imageResource = android.R.drawable.ic_lock_idle_lock
                onClick { Snackbar.make(this@floatingActionButton, "wow", 3000).show() }
            }.lparams {
                margin = dip(5)
                alignParentBottom()
                alignParentRight()
            }
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.cameraState }.skipRepeats()
        }
    }

    override fun newState(state: CameraState) {
        val latPrefix = if (state.lat >= 0) "北緯" else "南緯"
        val lonPrefix = if (state.lon >= 0) "東經" else "西經"

        val lat = state.lat
                .let { Math.abs(it) }
                .let { "%.6f".format(it) }
                .run { dropLast(3) + "-" + takeLast(3) }
        val lon = state.lon
                .let { Math.abs(it) }
                .let { "%.6f".format(it) }
                .run { dropLast(3) + "-" + takeLast(3) }

        coordinate?.text = "$latPrefix $lat 度\n$lonPrefix $lon 度"
    }
}