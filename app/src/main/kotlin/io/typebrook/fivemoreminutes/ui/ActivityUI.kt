package io.typebrook.fivemoreminutes.ui

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetBehavior.*
import android.view.Gravity
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.pengrad.mapscaleview.MapScaleView
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.services.android.telemetry.permissions.PermissionsManager
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dialog.CoordInputDialog
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.utils.*
import io.typebrook.fmmcore.map.*
import io.typebrook.fmmcore.realm.projection.*
import io.typebrook.fmmcore.realm.projection.CoordRefSys.Companion.WGS84
import io.typebrook.fmmcore.redux.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.coordinatorLayout
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import tw.geothings.geomaptool.offline_map.OfflineListDialog
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */

class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<Boolean> {

    private lateinit var activity: Activity

    lateinit var mapContainer: FrameLayout
    private lateinit var layers: ImageView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var coordinate: TextView
    private lateinit var scaleBar: MapScaleView
    private lateinit var gpsOn: ImageView
    private lateinit var gpsOff: ImageView
    private lateinit var buttonSet: LinearLayout
    private lateinit var zoomText: TextView
    private lateinit var header: _FrameLayout

    private val components by lazy { listOf(layers, coordinate, gpsOn, gpsOff, buttonSet) }

    override fun newState(state: Boolean) {
        bottomSheetBehavior.run {
            isHideable = state
            this.state = if (state) STATE_HIDDEN else STATE_COLLAPSED
        }
        components.forEach {
            it.visibility = if (state) View.INVISIBLE else View.VISIBLE
        }
    }

    private val coordPrinter = object : StoreSubscriber<CrsState> {
        var coordConverter: CoordConverter = { xyPair -> xyPair }
        var textPrinter: CoordPrinter = xy2IntString

        operator fun invoke(xy: XYPair): String {
            val convertedXY = coordConverter(xy)
            val xyString = textPrinter(convertedXY)
            val crsState = mainStore.state.crsState
            return when (crsState.crs) {
                WGS84 -> "${xyString.first}\n${xyString.second}"
                TaipowerCrs -> "${TaipowerCrs.displayName}: ${TaipowerCrs.mask(convertedXY)}"
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
            updateCamera(mainStore.state.currentCamera)
        }
    }

    private val mapSubscriber = object : StoreSubscriber<List<MapInfo>> {
        override fun newState(state: List<MapInfo>) {
            val currentMap = mainStore.state.currentMap
            if (currentMap.locating) {
                gpsOn.imageResource = R.drawable.ic_gps_fixed_black_24dp
                // TODO use new method when min API = 23
                gpsOn.setColorFilter(activity.resources.getColor(R.color.googleBlue))
                gpsOff.visibility = VISIBLE
            } else {
                gpsOn.imageResource = R.drawable.ic_gps_not_fixed_black_24dp
                gpsOn.clearColorFilter()
                gpsOff.visibility = INVISIBLE
            }
        }
    }

    private val cameraSubscriber = object : StoreSubscriber<CameraState> {
        override fun newState(state: CameraState) {
            val (lat, lon, zoom) = state
            coordinate.text = coordPrinter(lon to lat)
            zoomText.text = zoom.with("%.1f")
            scaleBar.update(state.zoom, state.lat)
        }
    }

    private val modeSubscriber = object : StoreSubscriber<Mode> {
        override fun newState(state: Mode) {
            header.removeAllViews()
            when (state) {
                Mode.Default -> header.setDefaultHeader()
                Mode.Focus -> header.setFocusHeader()
            }
        }
    }

    private fun updateCamera(camera: CameraState) {
        cameraSubscriber.newState(camera)
    }

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        activity = owner
        coordinatorLayout {
            mapContainer = frameLayout { id = id_map_container }.lparams {
                //                behavior = CollapseBehavior<FrameLayout>()
                anchorId = id_sheet
                anchorGravity = Gravity.TOP
                gravity = Gravity.TOP
            }

            // Shadow above bottom sheet
            imageView(R.drawable.shape_gradient_top_shadow).lparams(width = matchParent, height = dip(12)) {
                anchorId = id_sheet
            }

            val bottomSheet = cardView {
                id = id_sheet
                backgroundColor = this@with.resources.getColor(R.color.googleFrame)
                verticalLayout {
                    frameLayout {
                        header = this
                        onClick {
                            val behavior = BottomSheetBehavior.from(this@cardView)
                            if (behavior.state == STATE_EXPANDED) {
                                behavior.state = STATE_COLLAPSED
                            } else {
                                behavior.state = STATE_EXPANDED
                            }
                        }
                    }.lparams(height = 150, width = matchParent)
                }
            }.lparams(width = matchParent, height = 760) {
                behavior = BottomSheetBehavior<View>().apply {
                    this.peekHeight = 150
                    this.isHideable = false
                    state = BottomSheetBehavior.STATE_COLLAPSED
                    bottomSheetBehavior = this
                }
            }

            // View shows coordinate
            cardView {
                backgroundColor = Color.parseColor("#80FFFFFF")
                cardElevation = dip(3).toFloat()
                coordinate = textView {
                    padding = dip(5)
                    onClick { CoordInputDialog().show(owner.fragmentManager, null) }
                    onLongClick {
                        val (lat, lon, zoom) = mainStore.state.currentXYZ
                        val gmmIntentUri = Uri.parse("geo:$lat,$lon?z=$zoom")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.`package` = "com.google.android.apps.maps"
                        if (mapIntent.resolveActivity(owner.packageManager) != null) {
                            owner.startActivity(mapIntent)
                        }
                    }
                }
            }.lparams(wrapContent) {
                anchorId = id_sheet
                anchorGravity = Gravity.CLIP_HORIZONTAL
                gravity = Gravity.TOP
                layoutTransition = LayoutTransition()
            }

            scaleBar = mapScaleBar {
                metersOnly()
            }.lparams(wrapContent) {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dip(35)
            }

            layers = imageView(R.drawable.ic_layers_black_24dp) {
                backgroundResource = R.drawable.btn_circle
                padding = dip(13)
                onClick {
                    selector("圖層", tileList.map { it.name }) { _, index ->
                        val selectedTile = tileList[index]
                        mainStore dispatch AddWebTile(selectedTile)
                    }
                }
            }.lparams {
                gravity = Gravity.END
                rightMargin = dip(9.8f)
                topMargin = dip(29.8f)
            }
            gpsOff = imageView(R.drawable.ic_gps_off_black_24dp) {
                backgroundResource = R.drawable.btn_circle
                padding = dip(13)
                visibility = INVISIBLE
                onClick {
                    mainStore dispatch DisableLocation()
                }
            }.lparams {
                gravity = Gravity.END
                rightMargin = dip(9.8f)
                topMargin = dip(90f)
            }

            gpsOn = imageView(R.drawable.ic_gps_not_fixed_black_24dp) {
                backgroundResource = R.drawable.btn_circle
                padding = dip(13)
                onClick {
                    if (!PermissionsManager.areLocationPermissionsGranted(owner)) {
                        PermissionsManager(owner).requestLocationPermissions(owner)
                        return@onClick
                    }
                    createLocationRequest {
                        mainStore dispatch EnableLocation()
                    }
                }
            }.lparams(width = dip(50), height = dip(50)) {
                anchorId = id_buttonSet
                anchorGravity = Gravity.CLIP_VERTICAL
                gravity = Gravity.TOP or Gravity.RIGHT
                rightMargin = dip(9.8f)
            }

            buttonSet = verticalLayout {
                id = id_buttonSet
                bottomPadding = dip(9.8f)

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
                imageView {
                    imageResource = R.drawable.ic_zoom_in_black_24dp
                    backgroundResource = R.drawable.mapbutton_background
                    padding = 20
                    onClick { mainStore dispatch ZoomBy(1f) }
                }
                imageView {
                    imageResource = R.drawable.ic_zoom_out_black_24dp
                    backgroundResource = R.drawable.mapbutton_background
                    padding = 15
                    onClick { mainStore dispatch ZoomBy(-1f) }
                }
            }.lparams(width = wrapContent, height = wrapContent) {
                anchorId = id_sheet
                anchorGravity = Gravity.END
                gravity = Gravity.TOP
                rightMargin = dip(9.8f)
//                layoutTransition = LayoutTransition()
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
        mainStore.run {
            subscribe(this@ActivityUI) { subscription ->
                subscription.select { it.hideComponent }.skipRepeats()
            }
            subscribe(coordPrinter) { subscription ->
                subscription.select { it.crsState }.skipRepeats()
            }
            subscribe(mapSubscriber) { subscription ->
                subscription.select { it.maps }.skipRepeats()
            }
            subscribe(cameraSubscriber) { subscription ->
                subscription.select { it.currentCamera }.skipRepeats()
            }
            subscribe(modeSubscriber) { subscription ->
                subscription.select { it.mode }.skipRepeats()
            }
        }

        childrenSequence().forEach {
            layoutTransition = LayoutTransition()
        }
    }

    companion object {
        val id_map_container = "id_map_container".hashCode()
        val id_sheet = "id_sheet".hashCode()
        val id_buttonSet = "id_buttonSet".hashCode()

        val displayList = listOf(
                "Google" to Display.Google,
                "Mapbox" to Display.MapBox,
                "Dual" to Display.Dual
        )

        val styleList: List<Tile>
            get() = mainStore.state.currentControl.styles + listOf(
                    "魯地圖" fromRoughWebTile "http://rudy-daily.tile.basecamp.tw/{z}/{x}/{y}.png",
                    "經建三版" fromWebTile "http://gis.sinica.edu.tw/tileserver/file-exists.php?img=TM25K_2001-jpg-{z}-{x}-{y}",
                    "Google Satellite" fromWebTile "https://khms1.googleapis.com/kh?v=746&hl=zh-TW&x={x}&y={y}&z={z}",
                    "OSM" fromWebTile "http://c.tile.openstreetmap.org/{z}/{x}/{y}.png"
            )

        val tileList: List<Tile> = listOf(
                "地圖產生器航跡" fromRoughWebTile "http://rs.happyman.idv.tw/map/gpxtrack/{z}/{x}/{y}.png",
                "雷達回波圖" fromWebImage "http://opendata.cwb.gov.tw/opendata/MSC/O-A0058-003.png" at ((118.0 to 26.4663) to (124.0 to 20.4663))
        )
    }
}