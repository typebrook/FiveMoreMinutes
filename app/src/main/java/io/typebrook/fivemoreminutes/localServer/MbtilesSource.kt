package io.typebrook.fivemoreminutes.localServer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.content.ContextCompat
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import org.jetbrains.anko.ctx
import org.jetbrains.anko.db.MapRowParser
import org.jetbrains.anko.db.RowParser
import org.jetbrains.anko.db.select
import java.io.File
import java.io.FileOutputStream
import java.sql.Blob


/**
 * Created by pham on 2018/1/7.
 */

sealed class MBTilesSourceError : Error() {
    class CouldNotReadFileError : MBTilesSourceError()
    class UnknownFormatError : MBTilesSourceError()
    class UnsupportedFormatError : MBTilesSourceError()
}

class MetadataParser : MapRowParser<Pair<String, String>> {
    override fun parseRow(columns: Map<String, Any?>): Pair<String, String> {
        return columns["name"] as String to columns["value"] as String
    }
}

class TilesParser : MapRowParser<ByteArray> {
    override fun parseRow(columns: Map<String, Any?>): ByteArray {
        return columns["tile_data"] as ByteArray
    }
}

class MBTilesSource(val fileName: String) {

    val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(fileName, null)
    var isVector = false
    var tileSize: Int? = null
    var layersJson: String? = ""
    var attributions: String? = ""
    var minZoom: Float? = null
    var maxZoom: Float? = null
    var bounds: LatLngBounds? = null

    init {
        try {
            val format = db.select("metadata")
                    .whereSimple("name = ?", "format")
                    .parseSingle(MetadataParser()).second

            when (format){
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
                .parseList(TilesParser())
                .run {if (!isEmpty()) get(0) else null}
    }

    companion object {
        val validRasterFormats = listOf("jpg", "png")
        val validVectorFormats = listOf("pbf", "mvt")
    }

    // MARK: Functions


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

fun MapView.add(ctx: Context, source: MBTilesSource){
    val server = MbtilesServer(ctx).apply {
        sources.add(source)
        start()
    }
}