package io.typebrook.fivemoreminutes.localServer

import android.util.Log
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.style.sources.Source
import org.jetbrains.exposed.sql.Database

/**
 * Created by pham on 2018/1/7.
 */

sealed class MBTilesSourceError : Error() {
    class CouldNotReadFileError : MBTilesSourceError()
    class UnknownFormatError : MBTilesSourceError()
    class UnsupportedFormatError : MBTilesSourceError()
}

class MBTilesSource(filePath: String) : Source() {

    lateinit var dataBase: Database

    var isVector = false
    var tileSize: Int? = null
    var layersJson: String? = ""
    var attributions: String? = ""
    var minZoom: Float? = null
    var maxZoom: Float? = null
    var bounds: LatLngBounds? = null
    
    init {
        try {
            dataBase = Database.connect(filePath, driver = "org.h2.Driver").apply {
                Log.d(this@MBTilesSource::class.java.simpleName, url)
            }


        } catch (error: MBTilesSourceError) {
            print(error.localizedMessage)
        }
    }


    companion object {
        val validRasterFormats = listOf("jpg", "png")
        val validVectorFormats = listOf("pbf", "mvt")
    }

    // MARK: Functions


}