package io.typebrook.fivemoreminutes.redux

import com.google.android.gms.maps.model.CameraPosition
import tw.geothings.rekotlin.Action

/**
 * Created by pham on 2017/9/20.
 */

// dispatch when camera move
data class CameraPositionChange(val position: CameraPosition): Action