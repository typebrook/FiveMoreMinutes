package io.typebrook.fivemoreminutes.ui

import android.support.v7.widget.CardView
import android.view.Gravity
import android.view.ViewManager
import android.widget.LinearLayout
import com.github.pengrad.mapscaleview.MapScaleView
import io.typebrook.fivemoreminutes.R
import io.typebrook.fivemoreminutes.dialog.MarkerDialog
import io.typebrook.fivemoreminutes.dialog.SaveMarkerDialog
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.realm
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

// Bottom Sheet Header for Default Mode
fun _FrameLayout.setDefaultHeader(): LinearLayout = linearLayout {
    topPadding = dip(6)

    // Select MapView
    verticalLayout {
        onClick {
            val activity = mainStore.state.activity ?: return@onClick
            activity.selector("選擇MapView", ActivityUI.displayList.map { it.first }) { _, index ->
                val selectedDisplay = ActivityUI.displayList[index].second
                mainStore.dispatch(SetDisplay(selectedDisplay))
            }
        }

        imageView(R.drawable.ic_map_black_24dp) {
            leftPadding = dip(28)
            rightPadding = dip(28)
            setColorFilter(resources.getColor(R.color.iconDefault))
        }.lparams(height = matchParent)

        textView("提供者"){
            textSize = 12f
            gravity = Gravity.CENTER_HORIZONTAL
        }.lparams(width = matchParent)
    }

    // online maps
    verticalLayout {
        onClick {
            val activity = mainStore.state.activity ?: return@onClick
            activity.selector("線上地圖", ActivityUI.styleList.map { it.name }) { _, index ->
                val selectedTile = ActivityUI.styleList[index]
                mainStore dispatch SetTile(selectedTile)
            }
        }

        imageView(R.drawable.ic_place_black_24dp) {
            leftPadding = dip(28)
            rightPadding = dip(28)
            setColorFilter(resources.getColor(R.color.iconDefault))
        }.lparams(height = matchParent)

        textView("底圖"){
            textSize = 12f
            gravity = Gravity.CENTER_HORIZONTAL
        }.lparams(width = matchParent)
    }

    // marker list
    verticalLayout {
        onClick {
            val activity = mainStore.state.activity ?: return@onClick
            MarkerDialog().show(activity.fragmentManager, null)
        }

        imageView(R.drawable.ic_place_black_24dp) {
            leftPadding = dip(28)
            rightPadding = dip(28)
            setColorFilter(resources.getColor(R.color.iconDefault))
        }.lparams(height = matchParent)

        textView("我的航點"){
            textSize = 12f
            gravity = Gravity.CENTER_HORIZONTAL
        }.lparams(width = matchParent)
    }
}.lparams(width = wrapContent, height = matchParent) {
    gravity = Gravity.CENTER
}

// Bottom Sheet Header for Focus Mode
fun _FrameLayout.setFocusHeader(): LinearLayout = linearLayout {
    val activity = mainStore.state.activity ?: return@linearLayout
    backgroundColor = activity.resources.getColor(R.color.googleBlue)

    view().lparams(weight = 1f) // Occupy left side to let rest of views align to right

    val focus = mainStore.state.currentControl.focus ?: return@linearLayout
    if (focus.isManaged) {
        imageView(R.drawable.ic_more_vert_white_24px) {
            leftPadding = dip(25)
            rightPadding = dip(25)
            onClick {
                activity.selector(focus.name, listOf("刪除")) { _, index ->
                    when (index) {
                        0 -> {
                            realm.executeTransaction { focus.deleteFromRealm() }
                            mainStore.state.currentControl.focus = null
                        }
                    }
                }
            }
        }.lparams(height = matchParent)

    } else {

        // save button
        imageView(R.drawable.ic_save_white_24dp) {
            leftPadding = dip(25)
            rightPadding = dip(25)
            onClick {
                SaveMarkerDialog().show(activity.fragmentManager, null)
            }
        }.lparams(height = matchParent)
    }

}.lparams(width = matchParent, height = matchParent)
