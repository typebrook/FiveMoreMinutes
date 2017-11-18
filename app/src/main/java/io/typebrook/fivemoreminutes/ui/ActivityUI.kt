package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.view.ViewManager
import android.widget.FrameLayout
import android.widget.TextView
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.nightonke.boommenu.ButtonEnum
import com.nightonke.boommenu.Piece.PiecePlaceEnum
import io.realm.Realm
import io.typebrook.fivemoreminutes.Dialog.CrsCreateDialog
import io.typebrook.fivemoreminutes.MainActivity
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.map.fromWebTile
import io.typebrook.fmmcore.projection.*
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.SetDisplay
import io.typebrook.fmmcore.redux.SetProjection
import io.typebrook.fmmcore.redux.SetTile
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import tw.geothings.rekotlin.StoreSubscriber

/**
 * Created by pham on 2017/9/21.
 */

class ActivityUI : AnkoComponent<MainActivity>, StoreSubscriber<CameraState> {

    private lateinit var coordinate: TextView
    private lateinit var zoomText: TextView

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
            this@ActivityUI.newState(mainStore.state.currentTarget)
        }
    }

    lateinit var mapContainer: FrameLayout

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {

        relativeLayout {

            mapContainer = frameLayout { id = ID_MAP_CONTAINER }

            coordinate = textView {
                padding = dip(5)
                backgroundColor = Color.parseColor("#80FFFFFF")
                onClick {
                    selector("座標系統", coordList.map { it.displayName } + "+ Add New",
                            { _, index ->
                                if (index > coordList.lastIndex) {
                                    CrsCreateDialog().show(owner.fragmentManager, null)
                                } else {
                                    val selectedProj = coordList[index]
                                    mainStore.dispatch(SetProjection(selectedProj))
                                }
                            })
                }
                onLongClick {
                    val crs = mainStore.state.datum
                    val realm = Realm.getDefaultInstance()
                    realm.executeTransaction {
                        if (crs.isManaged) {
                            mainStore.dispatch(SetProjection(WGS84_Degree))
                            crs.deleteFromRealm()
                        }
                    }
                }
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
            }.lparams {
                alignParentBottom()
                alignParentLeft()
            }
        }
    }.apply {
        mainStore.subscribe(this@ActivityUI) { subscription ->
            subscription.select { it.currentTarget }.skipRepeats()
        }
        mainStore.subscribe(coordPrinter) { subscription ->
            subscription.select { it.datum }.skipRepeats()
        }
    }

    override fun newState(state: CameraState) {
        val (lat, lon, _) = state
        coordinate.text = coordPrinter(lon to lat)
    }

    private inline fun ViewManager.boomMenuButton(init: BoomMenuButton.() -> Unit): BoomMenuButton =
            ankoView({ BoomMenuButton(it, null) }, theme = 0, init = init)

    companion object {
        val ID_MAP_CONTAINER = 1000

        val displayList = listOf(
                "Google" to Display.Google,
                "Mapbox" to Display.MapBox,
                "Dual" to Display.Dual
        )

        val tileList: List<Tile>
            get() = mainStore.state.currentMap.mapControl.styles + listOf(
                    "魯地圖" fromWebTile "http://rudy-daily.tile.basecamp.tw/{z}/{x}/{y}.png",
                    "經建三版" fromWebTile "http://gis.sinica.edu.tw/tileserver/file-exists.php?img=TM25K_2001-jpg-{z}-{x}-{y}"
            )

        val coordList: List<Datum>
            get() {
                val realm = Realm.getDefaultInstance()
                val crsInRealm = realm.where(Datum::class.java).findAll().toList()

                return listOf(WGS84_Degree, WGS84_DMS, TWD97, TWD67) + crsInRealm
            }
    }
}