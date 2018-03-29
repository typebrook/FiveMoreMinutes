package io.typebrook.fivemoreminutes.utils

import io.realm.Realm
import io.realm.kotlin.where
import io.typebrook.fmmcore.realm.geometry.rMarker

/**
 * Created by pham on 2018/3/28.
 */

val markerList: List<rMarker>
    get() {
        val realm = Realm.getDefaultInstance()
        return realm.where<rMarker>().findAll().toList()
    }