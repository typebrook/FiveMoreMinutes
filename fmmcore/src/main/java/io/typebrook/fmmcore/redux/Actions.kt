package io.typebrook.fmmcore.redux

import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.projection.Datum
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

class Nothing : Action

data class AddMap(val map: MapControl) : Action
data class RemoveMap(val map: MapControl) : Action
data class FocusMap(val map: MapControl) : Action

class EnableLocation : Action
class DisableLocation : Action
class DidSwitchLocation(val isEnabled: Boolean) : Action

data class UpdateCurrentTarget(val holder: MapControl, val camera: CameraState) : Action

data class ZoomBy(val value: Float) : Action
class TargetBackward : Action
class GrantCameraSave : Action
class BlockCameraSave : Action

data class SetDisplay(val display: Display) : Action
data class SetTile(val tile: Tile?) : Action
data class DidFinishSetTile(val tile: Tile?) : Action
data class SetProjection(val coordSystem: Datum) : Action

class SwitchMap : Action // Not Valid for now
