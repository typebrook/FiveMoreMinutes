package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.support.design.widget.AppBarLayout
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.view.ViewCompat
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.github.pengrad.mapscaleview.MapScaleView
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.services.android.telemetry.permissions.PermissionsManager
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton
import com.nightonke.boommenu.ButtonEnum
import com.nightonke.boommenu.Piece.PiecePlaceEnum
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dialog.CoordInputDialog
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.utils.*
import io.typebrook.fmmcore.map.*
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.projection.CoordRefSys.Companion.WGS84
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import tw.geothings.geomaptool.offline_map.OfflineListDialog
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */

class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    lateinit var mapContainer: FrameLayout
    private lateinit var coordinate: TextView
    private lateinit var scaleBar: MapScaleView
    private lateinit var gpsOn: ImageView
    private lateinit var gpsOff: ImageView

    private lateinit var layers: ImageView
    private lateinit var zoomText: TextView
    private lateinit var zoomIn: ImageView
    private lateinit var zoomOut: ImageView

    private var isHide = false
    private val components by lazy { listOf(coordinate, gpsOn, gpsOff, zoomText, zoomIn, zoomOut) }

    private val coordPrinter = object : StoreSubscriber<CrsState> {
        var coordConverter: CoordConverter = { xyPair -> xyPair }
        var textPrinter: CoordPrinter = xy2IntString

        operator fun invoke(xy: XYPair): String {
            val xyString = coordConverter(xy).let { textPrinter(it) }
            val crsState = mainStore.state.crsState
            return when (crsState.crs) {
                WGS84 -> xyString.run { "$first\n$second" }
                else -> xyString.run {
                    if (crsState.isLonLat) "${crsState.crs.displayName}:\n$first\n$second"
                    else "${crsState.crs.displayName}: $first, $second"
                }
            }
        }

        override fun newState(state: CrsState) {
            coordConverter = CoordRefSys.generateConverter(WGS84, state.crs)
            textPrinter = when (state.coordExpr) {
                Expression.Int -> xy2IntString
                Expression.Degree -> xy2DegreeString
                Expression.DegMin -> xy2DegMinString
                Expression.DMS -> xy2DMSString
            }
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
        coordinatorLayout {

            mapContainer = frameLayout { id = ID_MAP_CONTAINER }.lparams {
                behavior = CollapseBehavior<FrameLayout>()
                anchorId = bla
                anchorGravity = Gravity.TOP
            }

            val sheet = verticalLayout {
                id = bla
                backgroundColor = Color.parseColor("#80FFFFFF")
                frameLayout {
                    backgroundColor = Color.parseColor("#80FFFF00")
                    onClick {
                        val behavior = BottomSheetBehavior.from(this@verticalLayout)
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }.lparams(height = 150, width = matchParent)
            }.lparams(width = matchParent, height = 700) {
                behavior = BottomSheetBehavior<View>().apply {
                    this.peekHeight = 150
                    this.isHideable = false
                    state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = R.color.transparentOnMap
                onClick { CoordInputDialog().show(owner.fragmentManager, null) }
            }.lparams(wrapContent) {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dip(10)
            }

            scaleBar = mapScaleBar {
                metersOnly()
            }.lparams(wrapContent) {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dip(10)
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
                            selector("線上地圖", styleList.map { it.name }) { _, index ->
                                val selectedTile = styleList[index]
                                mainStore dispatch SetTile(selectedTile)
                            }
                        })
                addBuilder(TextOutsideCircleButton.Builder()
                        .normalText("切換工具可見度")
                        .listener {
                            isHide = !isHide
                            components.forEach { it.visibility = if (isHide) View.INVISIBLE else View.VISIBLE }
                            owner.window.decorView.systemUiVisibility = if (isHide) View.SYSTEM_UI_FLAG_FULLSCREEN else View.VISIBLE

                        })
                onClick {
                    val behavior = BottomSheetBehavior.from(sheet)
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }.lparams {
                anchorId = bla
                anchorGravity = Gravity.TOP
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
                gravity = Gravity.END
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
                gravity = Gravity.END
                rightMargin = dip(9.8f)
                topMargin = dip(53f)
            }

            verticalLayout {
                bottomPadding = dip(9.8f)
                layers = imageView {
                    imageResource = R.drawable.ic_layers_black_24dp
                    backgroundResource = R.drawable.mapbutton_background
                    padding = 20
                    onClick {
                        selector("圖層", tileList.map { it.name }) { _, index ->
                            val selectedTile = tileList[index]
                            mainStore.dispatch(AddWebTile(selectedTile))
                        }
                    }
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
                }
                zoomIn = imageView {
                    imageResource = R.drawable.ic_zoom_in_black_24dp
                    backgroundResource = R.drawable.mapbutton_background
                    padding = 20
                    onClick { mainStore dispatch ZoomBy(1f) }
                }
                zoomOut = imageView {
                    imageResource = R.drawable.ic_zoom_out_black_24dp
                    backgroundResource = R.drawable.mapbutton_background
                    padding = 15
                    onClick { mainStore dispatch ZoomBy(-1f) }
                }
            }.lparams(width = wrapContent, height = wrapContent) {
                anchorId = bla
                anchorGravity = Gravity.END
                gravity = Gravity.TOP
                rightMargin = dip(9.8f)
            }

//            cameraScroller = bottomSheet {
//                backgroundColor = Color.DKGRAY
//                this.toggle()
//                linearLayout {
//                    textView {
//                        text = "previous"
//                        textSize = 16f
//                        textColor = Color.WHITE
//                        leftPadding = 25
//                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
//                        onClick {
//                            mainStore dispatch TargetBackward()
//                        }
//                    }.lparams(width = 0, height = matchParent, weight = 1f)
//                    textView {
//                        text = "next"
//                        textSize = 16f
//                        textColor = Color.WHITE
//                        rightPadding = 25
//                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
//                        onClick {
//                            mainStore dispatch TargetForward()
//                        }
//                    }.lparams(width = 0, height = matchParent, weight = 1f)
//                }
//            }.lparams(width = matchParent, height = 150) {
//                gravity = Gravity.BOTTOM
//            }
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.currentCamera }.skipRepeats()
        }
        mainStore.subscribe(coordPrinter) { subscription ->
            subscription.select { it.crsState }.skipRepeats()
        }
        mainStore.subscribe(mapSubscriber) { subscription ->
            subscription.select { it.maps }.skipRepeats()
        }
    }

    override fun newState(state: CameraState) {
        val (lat, lon, zoom) = state
        coordinate.text = coordPrinter(lon to lat)
//        zoomText.text = "${zoom.toInt()}"
        zoomText.text = zoom.with("%.1f")

        scaleBar.update(state.zoom, state.lat)
    }

    companion object {
        val ID_MAP_CONTAINER = "ID_MAP_CONTAINER".hashCode()
        val bla = "bla".hashCode()

        val displayList = listOf(
                "Google" to Display.Google,
                "Mapbox" to Display.MapBox,
                "Dual" to Display.Dual
        )

        val styleList: List<Tile>
            get() = mainStore.state.currentControl.styles + listOf(
                    "魯地圖" fromRoughWebTile "http://rudy-daily.tile.basecamp.tw/{z}/{x}/{y}.png",
                    "經建三版" fromWebTile "http://gis.sinica.edu.tw/tileserver/file-exists.php?img=TM25K_2001-jpg-{z}-{x}-{y}",
                    "Google Satellite" fromWebTile "https://khms1.googleapis.com/kh?v=746&hl=zh-TW&x={x}&y={y}&z={z}"
            )

        val tileList: List<Tile> = listOf(
                "地圖產生器航跡" fromRoughWebTile "http://rs.happyman.idv.tw/map/gpxtrack/{z}/{x}/{y}.png",
                "雷達回波圖" fromWebImage "http://opendata.cwb.gov.tw/opendata/MSC/O-A0058-003.png" at ((118.0 to 26.4663) to (124.0 to 20.4663))
        )
    }
}