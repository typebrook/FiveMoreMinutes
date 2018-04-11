package io.typebrook.fmmcore.realm.geometry

import io.realm.RealmObject
import io.realm.annotations.RealmClass
import java.util.*

/**
 * Created by pham on 2018/3/2.
 */

@RealmClass
open class rMarker(
        var name: String? = null,
        var lat: Double = 0.0,
        var lon: Double = 0.0,
        var date: Date = Date()
) : RealmObject()