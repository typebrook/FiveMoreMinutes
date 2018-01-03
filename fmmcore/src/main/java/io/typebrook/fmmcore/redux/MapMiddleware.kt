package io.typebrook.fmmcore.redux

import android.os.Handler
import android.os.Looper
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.projection.Expression
import tw.geothings.rekotlin.Action
import kotlin.reflect.KClass

/**
 * Created by pham on 2017/9/28.
 */
class MapMiddleware : SpawningMiddleware<State>() {

    @Suppress("UNCHECKED_CAST")
    override fun handlers(): List<Pair<KClass<Action>, ActionHandler<State>>> {
        return listOf(
                AddMap::class as KClass<Action> to addMap,
                SetTile::class as KClass<Action> to setTile,
                AddWebTile::class as KClass<Action> to addWebTile,
                ZoomBy::class as KClass<Action> to zoomMap,
                EnableLocation::class as KClass<Action> to enableLocation,
                DisableLocation::class as KClass<Action> to disableLocation
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun transformers(): List<Pair<KClass<Action>, ActionTransformer<State>>> {
        return listOf(
                UpdateCurrentTarget::class as KClass<Action> to updateCurrentTarget,
                SetCrsState::class as KClass<Action> to setCoordRefSys
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun spawners(): List<Pair<KClass<Action>, ActionSpawner<State>>> {
        return listOf(
                TargetBackward::class as KClass<Action> to cameraPositionBackward
        )
    }

    // Handlers
    private val addMap: ActionHandler<State> = handler@ { action, _ ->
        val map = (action as? AddMap)?.map ?: return@handler

        map.setStyle(null)
    }

    private val setTile: ActionHandler<State> = handler@ { action, getState ->
        val mapControl = getState()?.currentControl ?: return@handler

        val tile = (action as? SetTile)?.tile ?: return@handler
        when (tile) {
            is Tile.WebTile -> mapControl.setWebTile(tile)
            is Tile.PrivateStyle -> mapControl.setStyle(tile)
        }
    }

    private val addWebTile: ActionHandler<State> = handler@ { action, getState ->
        val mapControl = getState()?.currentControl ?: return@handler

        val tile = (action as? AddWebTile)?.tile ?: return@handler
        mapControl.addWebTile(tile)
    }

    private val zoomMap: ActionHandler<State> = handler@ { action, getState ->
        val mapControl = getState()?.currentControl ?: return@handler

        val value = (action as? ZoomBy)?.value ?: return@handler
        mapControl.zoomBy(value)
    }

    private val enableLocation: ActionHandler<State> = handler@ { _, getState ->
        val mapControl = getState()?.currentControl ?: return@handler
        mapControl.enableLocation()
    }

    private val disableLocation: ActionHandler<State> = handler@ { _, getState ->
        val mapControl = getState()?.currentControl ?: return@handler
        mapControl.disableLocation()
    }

    // Transformer
    private val updateCurrentTarget: ActionTransformer<State> = transformer@ { action, getState ->
        action as? UpdateCurrentTarget ?: return@transformer Nothing()

        if (action.holder == getState()?.currentControl) {
            val otherMaps = getState()?.maps?.map { it.mapControl }?.filter { it != action.holder }
            otherMaps?.forEach { it.moveCamera(action.camera) }
            action
        } else
            Nothing()
    }

    private val setCoordRefSys: ActionTransformer<State> = transformers@ { action, getState ->
        action as? SetCrsState ?: return@transformers action
        val newCrs = action.crs
        val oldCrs = getState()?.crsState?.crs ?: return@transformers action

        if (newCrs.isLonLat != oldCrs.isLonLat) {
            val expression = if (newCrs.isLonLat) Expression.Degree else Expression.Int
            return@transformers SetCrsState(newCrs, expression)
        }

        action
    }

    // Spawner
    private val cameraPositionBackward: ActionSpawner<State> = spawner@ { _, getState, callback ->
        val mapControl = getState()?.currentControl ?: return@spawner Nothing()

        mapControl.run {
            if (cameraStatePos == 0) return@run
            else cameraStatePos -= 1
            animateCamera(cameraQueue[cameraStatePos], 400)
        }

        Handler(Looper.getMainLooper()).postDelayed({ callback(GrantCameraSave()) }, 1000)
        BlockCameraSave()
    }
}