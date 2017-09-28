package io.typebrook.fivemoreminutes.redux

import android.os.Handler
import android.os.Looper
import android.util.Log
import tw.geothings.rekotlin.Action
import tw.geothings.rekotlin.Middleware
import tw.geothings.rekotlin.StateType
import kotlin.reflect.KClass

/**
 * Created by pham on 2017/9/28.
 */
class MyMiddleware : SpawningMiddleware<State>() {

    @Suppress("UNCHECKED_CAST")
    override fun spawners(): List<Pair<KClass<Action>, ActionSpawner<State>>> {
        return listOf(CameraPositionBackward::class as KClass<Action> to dealCameraSave)
    }

    private val dealCameraSave: ActionSpawner<State> = { action, _, callback ->
        Handler(Looper.getMainLooper()).postDelayed({ callback(GrantCameraSave()) }, 1000)
        action
    }
}