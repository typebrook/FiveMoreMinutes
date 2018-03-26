package io.typebrook.fivemoreminutes.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import io.typebrook.fivemoreminutes.mainStore

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

// request user to turn on location function
// ref: https://developer.android.com/training/location/change-location-settings.html
fun createLocationRequest(successHandler: () -> Unit = {}) {
    val activity = mainStore.state.activity ?: return

    val locationRequest = LocationRequest().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

    val client: SettingsClient = LocationServices.getSettingsClient(activity)
    val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

    task.addOnSuccessListener { _ ->
        successHandler()
    }

    task.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            // Location settings are not satisfied, but this can be fixed
            // by showing the user a dialog.
            try {
                // Show the dialog by calling startResolutionForResult(),
                // and check the result in onActivityResult().
                exception.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
            } catch (sendEx: IntentSender.SendIntentException) {
                // Ignore the error.
            }
        }
    }
}

const val REQUEST_CHECK_SETTINGS = 0x003

