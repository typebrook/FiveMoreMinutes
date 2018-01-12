package io.typebrook.fivemoreminutes.localServer

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.*
import java.net.ServerSocket
import java.net.Socket


/**
 * Created by pham on 2018/1/7.
 */

open class MbtilesServer(private val ctx: Context, val source: MBTilesSource?) : ServerSocket(7579), Runnable {

    var socket: Socket? = null

    override fun run() {
        bg {
            socket = accept()
            ctx.runOnUiThread { ctx.toast(this@MbtilesServer.localSocketAddress.toString()) }
            handle()
        }
    }

    fun stop() {
        ctx.toast("stop")
        socket?.close()
        socket = null
    }

    @Throws
    private fun handle() {
        val reader by lazy { BufferedReader(InputStreamReader(socket?.getInputStream())) }
        var output: PrintStream? = null
        try {
            var route: String? = null

            // Read HTTP headers and parse out the route.
            do {
                val line = reader.readLine()
                if (line.startsWith("GET /")) {
                    route = line.substringAfter("GET /")
                    break
                }
            } while (!line.isEmpty())

            // Output stream that we send the response to
            output = PrintStream(socket?.getOutputStream())

            // Prepare the content to send.
            if (null == route) {
                writeServerError(output)
                return
            }
            val bytes = loadContent(route)
            if (null == bytes) {
                writeServerError(output)
                return
            }

            // Send out the content.
            output.apply {
                println("HTTP/1.0 200 OK")
                println("Content-Type: " + detectMimeType(route))
                println("Content-Length: " + bytes.size)
                println()
                write(bytes)
                flush()
            }
        } finally {
            if (null != output) output.close()
            reader.close()
        }
    }


    @Throws
    private fun loadContent(fileName: String = "test.jpg"): ByteArray? {
        val input = ctx.assets.open(fileName)
        try {
            val output = ByteArrayOutputStream()
            output.write(input.readBytes())
            output.flush()
            return output.toByteArray()
        } catch (e: FileNotFoundException) {
        } finally {
            input?.run { close() }
        }
        return null
    }

    private fun writeServerError(output: PrintStream) {
        output.println("HTTP/1.0 500 Internal Server Error")
        output.flush()
    }

    private fun detectMimeType(fileName: String): String? = when {
        fileName.isEmpty() -> null
        fileName.endsWith(".jpg") -> "image/jpg"
        fileName.endsWith(".png") -> "image/png"
        else -> "application/octet-stream"
    }

}