package io.typebrook.fivemoreminutes.ui

import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.view.View
import android.view.ViewGroup

/**
 * Created by pham on 2018/1/29.
 */

class CollapseBehavior<V : ViewGroup> : CoordinatorLayout.Behavior<V>() {

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        if (isBottomSheet(dependency)) {
            val behavior = (dependency.layoutParams as CoordinatorLayout.LayoutParams).behavior as BottomSheetBehavior<*>?

            val peekHeight = behavior?.peekHeight ?: 0
            // The default peek height is -1, which
            // gets resolved to a 16:9 ratio with the parent
            val actualPeek = if (peekHeight >= 0) peekHeight else (parent.height * 1.0 / 16.0 * 9.0).toInt()
            if (dependency.top > actualPeek) {
                // Only perform translations when the
                // view is between "hidden" and "collapsed" states
                val dy = dependency.top - parent.height
                child.translationY = dy.toFloat() + actualPeek
//                ViewCompat.setTranslationZ(child, dy.toFloat())
//                child.translationZ = dy.toFloat()
                return true
            }
        }

        return false
    }

    private fun isBottomSheet(view: View): Boolean {
        val lp = view.layoutParams
        return lp is CoordinatorLayout.LayoutParams && lp.behavior is BottomSheetBehavior<*>
    }
}