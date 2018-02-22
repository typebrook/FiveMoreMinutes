package io.typebrook.fivemoreminutes.dialog

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.offline.*
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fivemoreminutes.mapfragment.MapboxMapFragment
import org.jetbrains.anko.*
import org.json.JSONObject

/**
 * Created by typeb on 9/28/2017.
 */
class DownloadDialog constructor() : DialogFragment() {

    private val mapControl = mainStore.state.currentMap.mapControl
    private val offlineManager by lazy { OfflineManager.getInstance(activity) }
    private var regionName = ""
    private var downloadingRegion: OfflineRegion? = null

    private val progressBar by lazy { UI {}.horizontalProgressBar() }
    private val progressText by lazy { UI {}.textView("0%") }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        regionName = args?.getString(ARG_REGIONNAME) ?: "Unnamed Region"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        downloadRegion(regionName)
        val content = UI {
            verticalLayout {
                addView(progressBar)
                addView(progressText.lparams { gravity = Gravity.CENTER_HORIZONTAL })
            }
        }.view
        return alert {
            title = "Downloading"
            customView = content
            isCancelable = false
            cancelButton {
                val activity = activity
                downloadingRegion?.apply {
                    setDownloadState(OfflineRegion.STATE_INACTIVE)
                    delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                        override fun onDelete() {
                            activity.toast("Download is cancelled")
                        }

                        override fun onError(error: String) {}
                    })
                }
            }
        }.build() as Dialog
    }

    private fun downloadRegion(regionName: String) {
        val styleUrl = (mapControl as? MapboxMapFragment)?.map?.styleUrl ?: return
        val bounds = mapControl.screenBound.run {
            LatLngBounds.from(first.first, first.second, second.first, second.second)
        }
//        val threshHold = 17.0
//        val currentZoom = mainStore.state.currentCamera.zoom
        val minZoom = 9.0
        val maxZoom = 16.0
        val pixelRatio = resources.displayMetrics.density
        val definition = OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, minZoom, maxZoom, pixelRatio)

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()
        val metadata: ByteArray
        try {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_FIELD_REGION_NAME, regionName)
            val json = jsonObject.toString()
            metadata = json.toByteArray()
        } catch (exception: Exception) {
            activity.toast("Download failed")
            dismiss()
            return
        }

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                Log.d(TAG, "Offline region created: " + regionName)
                launchDownload(offlineRegion)
            }

            override fun onError(error: String) {
                Log.e(TAG, "Error: " + error)
            }
        })
    }

    private fun launchDownload(offlineRegion: OfflineRegion) {
        this.downloadingRegion = offlineRegion
        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Compute a percentage
                val percentage = if (status.requiredResourceCount >= 0)
                    100.0 * status.completedResourceCount / status.requiredResourceCount
                else
                    0.0

                if (status.isComplete) {
                    // Download complete
                    activity.toast("Download completed")
                    this@DownloadDialog.dismiss()
                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    // Switch to determinate state
                    val percentageInt = percentage.toInt()
                    progressBar.progress = percentageInt
                    progressText.text = "$percentageInt% (${status.completedResourceCount}/${status.requiredResourceCount})"
                }

                // Log what is being currently downloaded
                Log.d(TAG, String.format("%s/%s resources; %s bytes downloaded.",
                        status.completedResourceCount.toString(),
                        status.requiredResourceCount.toString(),
                        status.completedResourceSize.toString()))
            }

            override fun onError(error: OfflineRegionError) {
                Log.e(TAG, "onError reason: " + error.reason)
                Log.e(TAG, "onError message: " + error.message)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Log.e(TAG, "Mapbox tile count limit exceeded: $limit")
                activity.toast("Mapbox tile count limit exceeded: $limit")
            }
        })
    }

    companion object {
        private val TAG = this::class.java.simpleName
        private val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
        private val ARG_REGIONNAME = "regionName"

        fun newInstance(regionName: String): DownloadDialog {
            val bundle = Bundle()
            bundle.putString(ARG_REGIONNAME, regionName)
            return DownloadDialog().apply { arguments = bundle }
        }
    }
}