package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.widget.TextView
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraState
import org.jetbrains.anko.*
import org.osgeo.proj4j.CoordinateTransform
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */
class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    private var coordinate: TextView? = null
    private var projTransform: CoordinateTransform? = null

    companion object {
        val ID_MAP_CONTAINER = 1000
    }

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {

        relativeLayout {

            verticalLayout {
                id = ID_MAP_CONTAINER
            }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = Color.parseColor("#80FFFFFF")
            }.lparams(wrapContent) {
                alignParentBottom()
                centerHorizontally()
                bottomMargin = dip(10)
            }
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.currentTarget }.skipRepeats()
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