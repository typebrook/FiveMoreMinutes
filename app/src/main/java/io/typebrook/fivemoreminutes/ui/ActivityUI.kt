package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.services.android.telemetry.permissions.PermissionsManager
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.nightonke.boommenu.ButtonEnum
import com.nightonke.boommenu.Piece.PiecePlaceEnum
import io.typebrook.fivemoreminutes.Dialog.CoordInputDialog
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.map.fromWebTile
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk25.coroutines.onClick
import tw.geothings.geomaptool.offline_map.OfflineListDialog
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */

class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    lateinit var mapContainer: FrameLayout
    private lateinit var coordinate: TextView
    private lateinit var gpsOn: ImageView
    private lateinit var gpsOff: ImageView
    private lateinit var zoomText: TextView
    private lateinit var zoomIn: ImageView
    private lateinit var zoomOut: ImageView

    private var isHide = false
    private val components by lazy { listOf(coordinate, zoomText, zoomIn, zoomOut) }

    private val coordPrinter = object : StoreSubscriber<Datum> {
        var coordConverter: CoordConverter = { xyPair -> xyPair }
        var textPrinter: CoordPrinter = defaultPrinter

        operator fun invoke(xy: XYPair): String {
            val xyString = coordConverter(xy).let { textPrinter(it) }
            val datum = mainStore.state.datum
            return when (datum) {
                WGS84_Degree -> xyString.run { "$second\n$first" }
                WGS84_DMS -> xyString.run { "$second\n$first" }
                TWD97 -> xyString.run { "TWD97: $first, $second" }
                TWD67 -> xyString.run { "TWD67: $first, $second" }
                else -> xyString.run { "${datum.displayName}\n$first\n$second" }
            }
        }

        override fun newState(state: Datum) {
            coordConverter = Datum.generateConverter(WGS84_Degree, state)
            textPrinter = state.printerº ?: defaultPrinter
            this@ActivityUI.newState(mainStore.state.currentCamera)
        }
    }

    private val mapSubscriber = object : StoreSubscriber<List<MapInfo>> {
        override fun newState(state: List<MapInfo>) {
            val currentMap = mainStore.state.currentMap
            if (currentMap.locating) {
                gpsOn.imageResource = R.drawable.ic_gps_fixed_black_24dp
                gpsOff.visibility = VISIBLE
            } else {
                gpsOn.imageResource = R.drawable.ic_gps_not_fixed_black_24dp
                gpsOff.visibility = INVISIBLE
            }
        }
    }

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {

        relativeLayout {

            mapContainer = frameLayout { id = ID_MAP_CONTAINER }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = Color.parseColor("#80FFFFFF")
                onClick { CoordInputDialog().show(owner.fragmentManager, null) }
            }.lparams(wrapContent) {
                alignParentBottom()
                centerHorizontally()
                bottomMargin = dip(10)
            }

            boomMenuButton {
                buttonEnum = ButtonEnum.TextOutsideCircle
                piecePlaceEnum = PiecePlaceEnum.DOT_3_3
                buttonPlaceEnum = ButtonPlaceEnum.SC_3_3
                isDraggable = true

                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("選擇排版")
                        .rotateText(false)
                        .listener {
                            selector("選擇MapView", displayList.map { it.first }) { _, index ->
                                val selectedDisplay = displayList[index].second
                                mainStore.dispatch(SetDisplay(selectedDisplay))
                            }
                        })
                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("選擇地圖")
                        .rotateText(false)
                        .listener {
                            selector("線上地圖", tileList.map { it.name }) { _, index ->
                                val selectedTile = tileList[index]
                                mainStore.dispatch(SetTile(selectedTile))
                            }
                        })
                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("切換工具可見度")
                        .listener {
                            isHide = !isHide
                            components.forEach { it.visibility = if (isHide) View.INVISIBLE else View.VISIBLE }
                            owner.window.decorView.systemUiVisibility = if (isHide) View.SYSTEM_UI_FLAG_FULLSCREEN else View.VISIBLE

                        })
            }.lparams {
                alignParentBottom()
                alignParentLeft()
            }

            gpsOn = imageView {
                imageResource = R.drawable.ic_gps_not_fixed_black_24dp
                backgroundResource = R.drawable.mapbutton_background
                padding = 25
                onClick {
                    if (!PermissionsManager.areLocationPermissionsGranted(owner)) {
                        PermissionsManager(owner).requestLocationPermissions(owner)
                    } else {
                        mainStore dispatch EnableLocation()
                    }
                }
            }.lparams {
                alignParentRight()
                rightMargin = dip(9.8f)
                topMargin = dip(9.8f)
            }
            gpsOff = imageView {
                imageResource = R.drawable.ic_gps_off_black_24dp
                backgroundResource = R.drawable.mapbutton_background
                padding = 25
                visibility = INVISIBLE
                onClick {
                    mainStore dispatch DisableLocation()
                }
            }.lparams {
                alignParentRight()
                rightMargin = dip(9.8f)
                topMargin = dip(53f)
            }

            zoomText = textView {
                backgroundResource = R.drawable.mapbutton_background
                gravity = Gravity.CENTER
                textSize = 20f
                onClick {
                    OfflineManager.getInstance(owner).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                        override fun onList(offlineRegions: Array<OfflineRegion>?) {
                            // Check result. If no regions have been
                            // downloaded yet, return empty array
                            OfflineListDialog().run {
                                this.offlineRegionsResult = offlineRegions ?: emptyArray()
                                show(owner.fragmentManager, null)
                            }
                        }

                        override fun onError(error: String) {}
                    })
                }
            }.lparams {
                alignParentRight()
                alignParentBottom()
                rightMargin = dip(9.8f)
                bottomMargin = dip(91f)
            }
            zoomIn = imageView {
                imageResource = R.drawable.ic_zoom_in_black_24dp
                backgroundResource = R.drawable.mapbutton_background
                padding = 20
                onClick { mainStore dispatch ZoomBy(1f) }
            }.lparams {
                alignParentRight()
                alignParentBottom()
                rightMargin = dip(9.8f)
                bottomMargin = dip(53f)
            }
            zoomOut = imageView {
                imageResource = R.drawable.ic_zoom_out_black_24dp
                backgroundResource = R.drawable.mapbutton_background
                padding = 15
                onClick { mainStore dispatch ZoomBy(-1f) }
            }.lparams {
                alignParentRight()
                alignParentBottom()
                rightMargin = dip(9.8f)
                bottomMargin = dip(15f)
            }
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.currentCamera }.skipRepeats()
        }
        mainStore.subscribe(coordPrinter) { subscription ->
            subscription.select { it.datum }.skipRepeats()
        }
        mainStore.subscribe(mapSubscriber) { subscription ->
            subscription.select { it.maps }.skipRepeats()
        }
    }

    override fun newState(state: CameraState) {
        val (lat, lon, zoom) = state
        coordinate.text = coordPrinter(lon to lat)
//        zoomText.text = "${zoom.toInt()}"
        zoomText.text = "%.1f".format(zoom)
    }

    private inline fun ViewManager.boomMenuButton(init: BoomMenuButton.() -> Unit): BoomMenuButton =
            ankoView({ BoomMenuButton(it, null) }, theme = 0, init = init)

    companion object {
        val ID_MAP_CONTAINER = "ID_MAP_CONTAINER".hashCode()

        val displayList = listOf(
                "Google" to Display.Google,
                "Mapbox" to Display.MapBox,
                "Dual" to Display.Dual
        )

        val tileList: List<Tile>
            get() = mainStore.state.currentMap.mapControl.styles + listOf(
                    "魯地圖" fromWebTile "http://rudy-daily.tile.basecamp.tw/{z}/{x}/{y}.png",
                    "經建三版" fromWebTile "http://gis.sinica.edu.tw/tileserver/file-exists.php?img=TM25K_2001-jpg-{z}-{x}-{y}",
                    "Google Satellite" fromWebTile "https://khms1.googleapis.com/kh?v=746&hl=zh-TW&x={x}&y={y}&z={z}"
            )
    }
}