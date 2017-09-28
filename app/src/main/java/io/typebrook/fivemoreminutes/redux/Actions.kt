package io.typebrook.fivemoreminutes.redux

import com.google.android.gms.maps.model.CameraPosition
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

data class CameraPositionChange(val cameraState: CameraState) : Action
data class CameraPositionSave(val cameraState: CameraState) : Action
class CameraPositionBackward : Action
class GrantCameraSave : Action