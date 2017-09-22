package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.widget.TextView
import com.google.android.gms.maps.model.CameraPosition
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.mainStore
import org.jetbrains.anko.*
import org.osgeo.proj4j.CoordinateTransform
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */
class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraPosition> {

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
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.lastCameraPosition }.skipRepeats()
        }
    }

    override fun newState(state: CameraPosition) {
        val lat = state.target.latitude.let { "%.6f".format(it) }
        val lon = state.target.longitude.let { "%.6f".format(it) }
        coordinate?.text = "北緯 ${lat} 度\n東經 ${lon} 度"
    }
}