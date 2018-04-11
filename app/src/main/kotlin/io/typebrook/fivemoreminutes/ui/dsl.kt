package io.typebrook.fivemoreminutes.ui

import android.graphics.Color
import android.support.v7.widget.CardView
import android.view.Gravity
import android.view.ViewManager
import android.widget.LinearLayout
import com.github.pengrad.mapscaleview.MapScaleView
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dialog.SaveMarkerDialog
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.utils.markerList
import io.typebrook.fmmcore.redux.CameraState
import io.typebrook.fmmcore.redux.SetDisplay
import io.typebrook.fmmcore.redux.SetTile
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * Created by pham on 2018/1/24.
 */

inline fun ViewManager.cardView(init: CardView.() -> Unit): CardView =
        ankoView({ CardView(it, null) }, theme = 0, init = init)

inline fun ViewManager.mapScaleBar(init: MapScaleView.() -> Unit): MapScaleView =
        ankoView({ MapScaleView(it, null) }, theme = 0, init = init)

fun _FrameLayout.setDefaultHeader(): LinearLayout = linearLayout {

    imageView(R.drawable.ic_map_black_24dp) {
        leftPadding = dip(25)
        rightPadding = dip(25)
        onClick {
            val activity = mainStore.state.activity ?: return@onClick
            activity.selector("選擇MapView", ActivityUI.displayList.map { it.first }) { _, index ->
                val selectedDisplay = ActivityUI.displayList[index].second
                mainStore.dispatch(SetDisplay(selectedDisplay))
            }
        }
    }
    imageView(R.drawable.ic_place_black_24dp) {
        leftPadding = dip(25)
        rightPadding = dip(25)
        onClick {
            val activity = mainStore.state.activity ?: return@onClick
            activity.selector("線上地圖", ActivityUI.styleList.map { it.name }) { _, index ->
                val selectedTile = ActivityUI.styleList[index]
                mainStore dispatch SetTile(selectedTile)
            }
        }
    }
    imageView(R.drawable.ic_place_black_24dp) {
        leftPadding = dip(25)
        rightPadding = dip(25)
        onClick {
            val list = markerList
            val nameList = markerList.map {
                it.name ?: it.date.toString().substringBefore(" GMT")
            }
            mainStore.state.activity?.selector("航點", nameList) { _, index ->
                val (lat, lon) = list[index].run { lat to lon }
                val target = CameraState(lat, lon, mainStore.state.currentCamera.zoom)
                mainStore.state.currentControl.animateCamera(target, 600)
            }
        }
    }
}.lparams(width = wrapContent, height = wrapContent) {
    gravity = Gravity.CENTER
}

fun _FrameLayout.setFocusHeader(): LinearLayout = linearLayout {
    val activity = mainStore.state.activity ?: return@linearLayout
    backgroundColor = activity.resources.getColor(R.color.googleBlue)

    imageView(R.drawable.ic_save_white_24dp) {
        leftPadding = dip(25)
        rightPadding = dip(25)
        onClick {
            SaveMarkerDialog().show(activity.fragmentManager, null)
        }
    }.lparams {
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }


}.lparams(width = matchParent, height = matchParent)
