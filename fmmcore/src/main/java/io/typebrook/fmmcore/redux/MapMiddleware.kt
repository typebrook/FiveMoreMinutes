package io.typebrook.fmmcore.redux

import android.os.Handler
import android.os.Looper
import io.typebrook.fmmcore.map.Tile
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
                ZoomBy::class as KClass<Action> to zoomMap
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun transformers(): List<Pair<KClass<Action>, ActionTransformer<State>>> {
        return listOf(
                UpdateCurrentTarget::class as KClass<Action> to updateCurrentTarget,
                EnableLocation::class as KClass<Action> to enableLocation,
                DisableLocation::class as KClass<Action> to disableLocation
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

    // Transformer
    private val updateCurrentTarget: ActionTransformer<State> = transformer@ { action, getState ->
        val updateTarget = action as? UpdateCurrentTarget ?: return@transformer Nothing()

        if (updateTarget.holder == getState()?.currentControl) {
            val otherMaps = getState()?.maps?.map { it.mapControl }?.filter { it != updateTarget.holder }
            otherMaps?.forEach { it.moveCamera(updateTarget.camera) }
            updateTarget
        } else
            Nothing()
    }

    private val enableLocation: ActionTransformer<State> = transformer@ { _, getState ->
        val mapControl = getState()?.currentControl ?: return@transformer Nothing()
        mapControl.enableLocation()
        return@transformer DidSwitchLocation(mapControl.locating)
    }

    private val disableLocation: ActionTransformer<State> = transformer@ { _, getState ->
        val mapControl = getState()?.currentControl ?: return@transformer Nothing()
        mapControl.disableLocation()
        return@transformer DidSwitchLocation(mapControl.locating)
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