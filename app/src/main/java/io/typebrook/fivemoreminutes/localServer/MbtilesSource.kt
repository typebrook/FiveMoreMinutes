package io.typebrook.fivemoreminutes.localServer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.mapbox.mapboxsdk.style.sources.VectorSource
import io.typebrook.fivemoreminutes.dispatch
import io.typebrook.fivemoreminutes.mainStore
import io.typebrook.fmmcore.map.fromWebTile
import io.typebrook.fmmcore.redux.SetTile
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
    class UnknownFormatError : MBTilesSourceError()
    class UnsupportedFormatError : MBTilesSourceError()
}

object MetadataParser : MapRowParser<Pair<String, String>> {
    override fun parseRow(columns: Map<String, Any?>): Pair<String, String> =
            columns["name"] as String to columns["value"] as String
}

object TilesParser : MapRowParser<ByteArray> {
    override fun parseRow(columns: Map<String, Any?>): ByteArray = columns["tile_data"] as ByteArray
}

class MBTilesSource(val filePath: String) {

    val id = filePath.substringAfterLast("/").substringBefore(".")
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

            when (format) {
                in validVectorFormats -> isVector = true
                in validRasterFormats -> isVector = false
                else -> throw MBTilesSourceError.UnknownFormatError()
            }

        } catch (error: MBTilesSourceError) {
            print(error.localizedMessage)
        }
    }

    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        return db.select("tiles")
                .whereArgs("(zoom_level = {z}) and (tile_column = {x}) and (tile_row = {y})", "z" to z, "x" to x, "y" to y)
                .parseList(TilesParser)
                .run { if (!isEmpty()) get(0) else null }
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

fun MapboxMap.add(ctx: Context, source: MBTilesSource) {
    if (source.id in this@add.sources.map { it.id }) {
        ctx.toast("source exists")
        return
    }
    
    MbtilesServer(ctx).apply {
        sources[source.id] = source
        start()
    }

    val requestUrl = "http://localhost:7579/${source.id}/{z}/{x}/{y}.${source.format}"
    if (source.isVector) {
        this@add.addSource(VectorSource(source.id, TileSet(null, requestUrl)))
    } else {
        this@add.addSource(RasterSource(source.id, TileSet(null, requestUrl), 126))
    }
}