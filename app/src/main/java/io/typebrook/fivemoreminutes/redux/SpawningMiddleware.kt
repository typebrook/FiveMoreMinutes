package io.typebrook.fivemoreminutes.redux

import android.os.Handler
import android.os.Looper
import tw.geothings.rekotlin.Action
import tw.geothings.rekotlin.DispatchFunction
import tw.geothings.rekotlin.StateType
import kotlin.reflect.KClass

/**
 * Created by hurden on 08/08/2017.
 * Copyright © 2016 GeoThings. All rights reserved.
 */

typealias TransformedAction = Action
typealias SpawnedAction = Action

typealias ActionHandler<State> = (Action, () -> State?) -> Unit
typealias ActionSpawner<State> = (Action, () -> State?, (SpawnedAction) -> Unit) -> TransformedAction?
typealias ActionTransformer<State> = (Action, () -> State?) -> TransformedAction

open class SpawningMiddleware<S : StateType> {

    open fun handlers(): List<Pair<KClass<Action>, ActionHandler<S>>> = emptyList()
    open fun spawners(): List<Pair<KClass<Action>, ActionSpawner<S>>> = emptyList()
    open fun transformers(): List<Pair<KClass<Action>, ActionTransformer<S>>> = emptyList()

    fun handle(dispatch: DispatchFunction, getState: () -> S?): (DispatchFunction) -> DispatchFunction {
        return { next ->
            middlewareHandler@ { action: Action ->

                this.handlers().forEach { handler ->
                    if (handler.first == action::class) {
                        handler.second(action, getState)
                    }
                }

                for (spawner in this.spawners()) {
                    if (spawner.first == action::class) {
                        val transformedAction = spawner.second(action, getState) { spawnedAction -> Handler(Looper.getMainLooper()).post { dispatch(spawnedAction) } }
                        next(transformedAction ?: action)
                        return@middlewareHandler
                    }
                }

                for (transformer in this.transformers()) {
                    if (transformer.first == action::class) {
                        next(transformer.second(action, getState))
                        return@middlewareHandler
                    }
                }

                next(action)
                return@middlewareHandler
            }
        }
    }
}