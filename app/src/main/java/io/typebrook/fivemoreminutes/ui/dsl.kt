package io.typebrook.fivemoreminutes.ui

import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewManager
import com.github.pengrad.mapscaleview.MapScaleView
import com.nightonke.boommenu.BoomMenuButton
import org.jetbrains.anko.custom.ankoView

/**
 * Created by pham on 2018/1/24.
 */

inline fun ViewManager.boomMenuButton(init: BoomMenuButton.() -> Unit): BoomMenuButton =
        ankoView({ BoomMenuButton(it, null) }, theme = 0, init = init)

inline fun ViewManager.mapScaleBar(init: MapScaleView.() -> Unit): MapScaleView =
        ankoView({ MapScaleView(it, null) }, theme = 0, init = init)