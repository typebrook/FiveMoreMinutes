package io.typebrook.fmmcore.realm.geometry

import io.realm.RealmObject
import io.realm.annotations.RealmClass

/**
 * Created by pham on 2018/3/2.
 */

@RealmClass
open class rMarker(
        var name: String? = null,
        var lat: Double = 0.0,
        var lon: Double = 0.0
) : RealmObject()