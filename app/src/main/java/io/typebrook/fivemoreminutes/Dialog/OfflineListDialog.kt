package tw.geothings.geomaptool.offline_map

import android.app.*
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import io.typebrook.fivemoreminutes.Dialog.DownloadDialog
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.redux.CameraState
import org.jetbrains.anko.UI
import org.jetbrains.anko.alert
import org.jetbrains.anko.editText
import org.jetbrains.anko.horizontalProgressBar
import org.json.JSONObject

/**
 * Created by typeb on 9/27/2017.
 */
class OfflineListDialog : DialogFragment() {

    private val mapControl = mainStore.state.currentMap.mapControl
    private var regionSelected = 0
    var offlineRegions = arrayOf<OfflineRegion>()
    private val deleteProgressBar by lazy { UI {}.horizontalProgressBar { visibility = View.GONE } }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val items = offlineRegions.map { getRegionName(it) }.toTypedArray()

        return AlertDialog.Builder(activity)
                .setTitle("Offline Maps Downloaded")
                .apply {
                    if (items.isNotEmpty()) {
                        setView(deleteProgressBar)
                        setSingleChoiceItems(items, 0) { _, which -> regionSelected = which }
                        setPositiveButton("GO TO") { _, _ ->
                            Toast.makeText(activity, items[regionSelected], Toast.LENGTH_LONG).show()

                            // Get the region bounds and zoom
                            val definition = offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition
                            val bounds = definition.bounds
                            val regionZoom = definition.minZoom

                            // Move camera to new position
                            mapControl.animateCamera(
                                    with(bounds.center) { CameraState(latitude, longitude, regionZoom.toFloat()) },
                                    800
                            )
                        }
                        setNeutralButton("Delete") { _, _ -> }
                    } else {
                        setTitle("No Offline Maps")
                    }
                }
                .setNegativeButton("Download Here") { _, _ ->
                    activity.alert {
                        val activity = activity
                        val defaultName = "Map${items.size + 1}"
                        val regionNameInput = UI {}.editText { hint = defaultName }
                        title = "Please Input Map Name"
                        customView = regionNameInput
                        isCancelable = false
                        positiveButton("Download") {
                            val regionName = regionNameInput.text.run { if (isBlank()) defaultName else toString() }
                            DownloadDialog.newInstance(regionName).show(activity.fragmentManager, null)
                        }
                    }.show()
                }
                .create() // here we create Dialog
                .apply {
                    // here we set the delete and download actions,
                    // so it won't jump out from this Dialog
                    setOnShowListener {
                        val deleteButton = getButton(DialogInterface.BUTTON_NEUTRAL)
                        deleteButton.setOnClickListener {
                            // Make deleteProgressBar indeterminate and
                            // set it to visible to signal that
                            // the deletion process has begun
                            deleteProgressBar.isIndeterminate = true
                            deleteProgressBar.visibility = View.VISIBLE

                            // Begin the deletion process
                            offlineRegions[regionSelected].delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                override fun onDelete() {
                                    // Once the region is deleted, remove the
                                    // deleteProgressBar and display a toast
                                    deleteProgressBar.visibility = View.INVISIBLE
                                    deleteProgressBar.isIndeterminate = false
                                    this@OfflineListDialog.dismiss()
                                    activity?.let { OfflineListDialog().show(fragmentManager, null) }
                                }

                                override fun onError(error: String) {
                                    deleteProgressBar.visibility = View.INVISIBLE
                                    deleteProgressBar.isIndeterminate = false
                                    Log.e(TAG, "Error: " + error)
                                }
                            })
                        }
                    }
                }
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        // Get the region name from the offline region metadata
        return try {
            val metadata = offlineRegion.metadata
            val json = String(metadata)
            val jsonObject = JSONObject(json)
            jsonObject.getString(JSON_FIELD_REGION_NAME)
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to decode metadata: " + exception.message)
            "Region ${offlineRegion.id}"
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
        private val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    }
}