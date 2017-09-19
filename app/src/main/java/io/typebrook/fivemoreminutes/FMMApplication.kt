package io.typebrook.fivemoreminutes

import android.app.Application
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

/**
 * Created by pham on 2017/9/20.
 */

var postion = CameraPosition(LatLng(24.782347, 121.035044), 15f, 0f, 0f)

class FMMApplication : Application() {
}