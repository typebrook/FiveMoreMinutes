package io.typebrook.fivemoreminutes.localServer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.style.sources.VectorSource
import org.jetbrains.anko.db.MapRowParser
import org.jetbrains.anko.db.select
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream


/**
 * Created by pham on 2018/1/7.
 */

sealed class MBTilesSourceError : Error() {
    class CouldNotReadFileError : MBTilesSourceError()
    class UnsupportedFormatError : MBTilesSourceError()
}

object MetadataParser : MapRowParser<Pair<String, String>> {
    override fun parseRow(columns: Map<String, Any?>): Pair<String, String> =
            columns["name"] as String to columns["value"] as String
}

object TilesParser : MapRowParser<ByteArray> {
    override fun parseRow(columns: Map<String, Any?>): ByteArray = columns["tile_data"] as ByteArray
}

class MBTilesSource(filePath: String, id: String? = null) {

    var id = id ?: filePath.substringAfterLast("/").substringBefore(".")
    val url get() = "http://localhost:8888/$id/{z}/{x}/{y}.$format"
    private val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(filePath, null)

    var isVector = false
    var format = ""
    var tileSize: Int? = null
    var layersJson: String? = ""
    var attributions: String? = ""
    var minZoom: Float? = null
    var maxZoom: Float? = null
    var bounds: LatLngBounds? = null

    init {
        try {
            format = db.select("metadata")
                    .whereSimple("name = ?", "format")
                    .parseSingle(MetadataParser).second

            isVector = when (format) {
                in validVectorFormats -> true
                in validRasterFormats -> false
                else -> throw MBTilesSourceError.UnsupportedFormatError()
            }

        } catch (error: MBTilesSourceError) {
            print(error.localizedMessage)
        }
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        return db.select("tiles")
                .whereArgs("(zoom_level = {z}) and (tile_column = {x}) and (tile_row = {y})",
                        "z" to z, "x" to x, "y" to y)
                .parseList(TilesParser)
                .run { if (!isEmpty()) get(0) else null }
    }

    fun activate() {
        val source = this
        MbtilesServer.apply {
            sources[source.id] = source
            if (!isRunning) start()
        }
    }

    companion object {
        val validRasterFormats = listOf("jpg", "png")
        val validVectorFormats = listOf("pbf", "mvt")
    }
}

fun getDBFromAsset(ctx: Context, fileName: String): SQLiteDatabase {
    val DB_PATH = ctx.filesDir.path

    //move the db to the designated path
    if (!File(DB_PATH + fileName).exists()) {
        val f = File(DB_PATH)
        if (!f.exists()) f.mkdir()

        try {
            val dbInputStream = ctx.assets.open(fileName)
            val dbOutputStream = FileOutputStream(DB_PATH + fileName)
            dbOutputStream.write(dbInputStream.readBytes())

            dbOutputStream.flush()
            dbOutputStream.close()
            dbInputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return SQLiteDatabase.openOrCreateDatabase(DB_PATH + fileName, null)
}