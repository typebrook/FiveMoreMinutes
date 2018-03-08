package io.typebrook.fivemoreminutes.utils

import android.content.Intent
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.redux.AddMarker
import io.typebrook.fmmcore.redux.UpdateCurrentTarget

/**
 * Created by pham on 2018/3/5.
 */

val intentHandler = { intent: Intent ->
    if (intent.data?.scheme == "geo") {
        val target = intent.dataString
                .substringAfter("geo:")
                .substringBefore("?")
                .split(',')
                .run { get(1).toDouble() to get(0).toDouble() }
        mainStore dispatch AddMarker(target)
    }
}