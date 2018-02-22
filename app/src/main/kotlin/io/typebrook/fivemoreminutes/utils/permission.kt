package io.typebrook.fivemoreminutes.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Created by pham on 2018/1/21.
 */

fun checkWriteExternal(ctx: Context): Boolean {
    val permission = ContextCompat.checkSelfPermission(ctx,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

    if (permission != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(ctx as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
        return false
    }
    return true
}