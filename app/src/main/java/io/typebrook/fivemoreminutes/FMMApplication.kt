package io.typebrook.fivemoreminutes

import android.app.Application
import io.typebrook.fmmcore.redux.MapMiddleware
import io.typebrook.fmmcore.redux.State
import io.typebrook.fmmcore.redux.reducer
import tw.geothings.rekotlin.Store

/**
 * Created by pham on 2017/9/20.
 */

val mainStore = Store(
        reducer = ::reducer,
        state = State(),
        middleware = listOf(MapMiddleware()::handle)
)

class FMMApplication : Application()