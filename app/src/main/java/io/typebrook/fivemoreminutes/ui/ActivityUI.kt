package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.view.ViewManager
import android.widget.FrameLayout
import android.widget.TextView
import com.nightonke.boommenu.BoomButtons.*
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.redux.CameraState
import org.jetbrains.anko.*
import org.osgeo.proj4j.CoordinateTransform
import tw.geothings.rekotlin.StoreSubscriber
import com.nightonke.boommenu.BoomMenuButton
import com.nightonke.boommenu.ButtonEnum
import com.nightonke.boommenu.Piece.PiecePlaceEnum
import org.jetbrains.anko.custom.ankoView
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.mapfragment.Display
import io.typebrook.fivemoreminutes.redux.SetDisplay
import io.typebrook.fivemoreminutes.redux.SetTile
import kotlinx.coroutines.experimental.android.UI
import tw.geothings.rekotlin.Action


/**
 * Created by pham on 2017/9/21.
 */
class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    private var coordinate: TextView? = null
    private var projTransform: CoordinateTransform? = null

    lateinit var mapContainer: FrameLayout

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {

        relativeLayout {

            mapContainer = frameLayout { id = ID_MAP_CONTAINER }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = Color.parseColor("#80FFFFFF")
            }.lparams(wrapContent) {
                alignParentBottom()
                centerHorizontally()
                bottomMargin = dip(10)
            }

            boomMenuButton {
                buttonEnum = ButtonEnum.TextOutsideCircle
                piecePlaceEnum = PiecePlaceEnum.DOT_2_2
                buttonPlaceEnum = ButtonPlaceEnum.SC_2_2
                isDraggable = true

                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("選擇排版")
                        .rotateText(false)
                        .listener {
                            selector("選擇MapView", displayList.map { it.first }) { _, index ->
                                mainStore.dispatch(SetDisplay(displayList[index].second))
                            }
                        })
                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("選擇地圖")
                        .rotateText(false)
                        .listener {
                            selector("線上地圖", tileList.map { it.first }) { _, index ->
                                mainStore.dispatch(SetTile(tileList[index].second))
                            }
                        })
            }.lparams {
                alignParentBottom()
                alignParentLeft()
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

    private inline fun ViewManager.boomMenuButton(init: BoomMenuButton.() -> Unit): BoomMenuButton {
        return ankoView({ BoomMenuButton(it, null) }, theme = 0, init = init)
    }

    companion object {
        val ID_MAP_CONTAINER = 1000

        val displayList = listOf(
                "Google" to Display.Google,
                "Mapbox" to Display.MapBox,
                "Dual" to Display.Dual
        )

        val tileList = listOf(
                "魯地圖" to "http://rudy-daily.tile.basecamp.tw/{z}/{x}/{y}.png",
                "經建三版" to "http://gis.sinica.edu.tw/tileserver/file-exists.php?img=TM25K_2001-jpg-{z}-{x}-{y}",
                "清空" to null
        )
    }
}