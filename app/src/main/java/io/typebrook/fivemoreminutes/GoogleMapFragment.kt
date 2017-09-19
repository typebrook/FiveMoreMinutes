package io.typebrook.fivemoreminutes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import org.jetbrains.anko.toast

/**
 * Created by pham on 2017/9/19.
 */
class GoogleMapFragment : MapFragment(), OnMapReadyCallback {

    init {
        getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap?) {
        toast("map ready")
    }
}