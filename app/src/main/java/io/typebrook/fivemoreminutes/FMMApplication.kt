package io.typebrook.fivemoreminutes

import android.app.Application
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.mapbox.mapboxsdk.Mapbox
import io.typebrook.fivemoreminutes.redux.State
import io.typebrook.fivemoreminutes.redux.reducer
import tw.geothings.rekotlin.Store

/**
 * Created by pham on 2017/9/20.
 */

val mainStore = Store(
        reducer = ::reducer,
        state = State()
)

class FMMApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Mapbox.getInstance(applicationContext, resources.getString(R.string.token_mapbox))
    }
}