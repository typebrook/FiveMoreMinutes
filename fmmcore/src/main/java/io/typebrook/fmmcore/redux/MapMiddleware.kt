package io.typebrook.fmmcore.redux

import android.os.Handler
import android.os.Looper
import io.typebrook.fmmcore.map.MapControl
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
                SetTile::class as KClass<Action> to setMapTile,
                ZoomBy::class as KClass<Action> to zoomMap
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun transformers(): List<Pair<KClass<Action>, ActionTransformer<State>>> {
        return listOf(
                UpdateCurrentTarget::class as KClass<Action> to updateCurrentTarget
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun spawners(): List<Pair<KClass<Action>, ActionSpawner<State>>> {
        return listOf(
                TargetBackward::class as KClass<Action> to cameraPositionBackward
        )
    }

    // Handlers
    private val addMap: ActionHandler<State> = handler@ { action, getState ->
        val map = (action as? AddMap)?.map ?: return@handler

        map.changeStyle(null)
    }

    private val setMapTile: ActionHandler<State> = handler@ { action, getState ->
        val mapControl: MapControl = getState()?.currentMap?.mapControl ?: return@handler

        val tile = (action as? SetTile)?.tileUrl ?: return@handler
        when (tile) {
            is Tile.WebTile -> mapControl.changeWebTile(tile.url)
            is Tile.PrivateStyle -> {
                mapControl.changeWebTile(null)
                mapControl.changeStyle(tile.value)
            }
        }
    }

    private val zoomMap: ActionHandler<State> = handler@ { action, getState ->
        val mapControl: MapControl = getState()?.currentMap?.mapControl ?: return@handler

        val value = (action as? ZoomBy)?.value ?: return@handler
        mapControl.zoomBy(value)
    }

    // Transformer
    private val updateCurrentTarget: ActionTransformer<State> = transformer@ { action, getState ->
        val updateTarget = action as? UpdateCurrentTarget ?: return@transformer Nothing()

        if (updateTarget.holder == getState()?.currentMap?.mapControl) {
            val otherMaps = getState()?.mapStates?.map { it.mapControl }?.filter { it != updateTarget.holder }
            otherMaps?.forEach { it.moveCamera(updateTarget.camera) }
            updateTarget
        } else
            Nothing()
    }

    // Spawner
    private val cameraPositionBackward: ActionSpawner<State> = spawner@ { _, getState, callback ->
        val mapControl: MapControl = getState()?.run { mapStates[currentMapNum].mapControl }
                ?: return@spawner Nothing()

        mapControl.run {
            if (cameraStatePos == 0) return@run
            else cameraStatePos -= 1
            animateCamera(cameraQueue[cameraStatePos], 400)
        }

        Handler(Looper.getMainLooper()).postDelayed({ callback(GrantCameraSave()) }, 1000)
        BlockCameraSave()
    }
}