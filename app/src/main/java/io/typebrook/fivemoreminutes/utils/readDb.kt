package io.typebrook.fivemoreminutes.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream

/**
 * Created by pham on 2018/1/10.
 */

fun readDb(ctx: Context) {
    val progressBar = ctx.indeterminateProgressDialog("reading").apply { show() }

    bg {
        val access = ctx.assets
        val buffer = ByteArray(1024)
        val DB_PATH = "/data/data/io.typebrook.fivemoreminutes/databases/"

        //move the db to the designated path
        if (!File(DB_PATH + "test.mbtiles").exists()) {
            val f = File(DB_PATH)
            if (!f.exists()) {
                f.mkdir()
            }
        }

        try {
            val dbInputStream = access.open("test.mbtiles")
            val dbOutputStream = FileOutputStream(DB_PATH + "test.mbtiles")
            ctx.runOnUiThread { ctx.toast("output from file") }

            dbOutputStream.write(dbInputStream.readBytes())
            dbOutputStream.flush()
            dbOutputStream.close()
            dbInputStream.close()
            ctx.runOnUiThread { ctx.toast("read all from file") }


        } catch (e: Exception) {
            e.printStackTrace()
            ctx.runOnUiThread {
                ctx.toast("fail to read")
                progressBar.dismiss()
            }
        }

        val db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + "test.mbtiles", null)
        ctx.runOnUiThread {
            ctx.toast(db.path)
            progressBar.dismiss()
        }
    }
}