package io.typebrook.fmmcore.redux

import android.app.Activity
import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.MapControl
import io.typebrook.fmmcore.map.Tile
import io.typebrook.fmmcore.projection.CoordRefSys
import io.typebrook.fmmcore.projection.Expression
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

class Nothing : Action

data class SetContext(val activity: Activity): Action

data class AddMap(val map: MapControl) : Action
data class RemoveMap(val map: MapControl) : Action
data class FocusMap(val map: MapControl) : Action

class EnableLocation : Action
class DisableLocation : Action
class DidSwitchLocation(val map: MapControl, val isEnabled: Boolean) : Action

data class UpdateCurrentTarget(val holder: MapControl, val camera: CameraState) : Action

data class ZoomBy(val value: Float) : Action
class TargetBackward : Action
class TargetForward : Action
class GrantCameraSave : Action
class BlockCameraSave : Action

data class SetDisplay(val display: Display) : Action
data class SetTile(val tile: Tile?) : Action
data class AddWebTile(val tile: Tile) : Action
data class SetCrsState(val crs: CoordRefSys, val expression: Expression? = null) : Action
data class SetCoordExpr(val expression: Expression) : Action

class SwitchMap : Action // Not Valid for now

class SwitchComponentVisibility : Action
