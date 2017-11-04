package io.typebrook.fmmcore.redux

import io.typebrook.fmmcore.map.Display
import io.typebrook.fmmcore.map.MapControl
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

class PureAction : Action

data class AddMap(val map: MapControl) : Action
data class RemoveMap(val map: MapControl) : Action
class SwitchMap : Action

data class UpdateCameraTarget(val holder: MapControl, val cameraState: CameraState) : Action

class CameraPositionBackward : Action
class GrantCameraSave: Action
class BlockCameraSave : Action


data class SetTile(val tileUrl: String?) : Action
data class SetDisplay(val display: Display) : Action
