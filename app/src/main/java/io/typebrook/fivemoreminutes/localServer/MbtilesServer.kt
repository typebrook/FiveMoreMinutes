package io.typebrook.fivemoreminutes.localServer

import android.content.Context
import android.util.Log
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.pow


/**
 * Created by pham on 2018/1/7.
 */

open class MbtilesServer(private val ctx: Context) : Runnable {

    var serverSocket: ServerSocket? = null
    var isRunning = false
    val sources: MutableList<MBTilesSource> = mutableListOf()

    fun start() {
        ctx.toast("start")
        isRunning = true
        Thread(this).start()
    }

    fun stop() {
        ctx.toast("stop")
        isRunning = false
        serverSocket?.close()
        serverSocket = null
    }

    override fun run() {
        try {
            serverSocket = ServerSocket(7579)
            while (isRunning) {
                val socket = serverSocket?.accept() ?: throw Error()
                handle(socket)
                socket.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("simpleServer", e.localizedMessage)
            ctx.runOnUiThread {
                toast("Localhost crashed")
                longToast(e.localizedMessage)
            }
        }
    }

    @Throws
    private fun handle(socket: Socket) {
        var reader: BufferedReader? = null
        var output: PrintStream? = null

        try {
            var route: String? = null
            reader = socket.getInputStream().bufferedReader()

            // Read HTTP headers and parse out the route.
            do {
                val line = reader.readLine() ?: ""
                if (line.startsWith("GET")) {
                    route = line.substringAfter("GET /").substringBefore(".")
                    Log.d("simpleServer", "line = $line")
                    Log.d("simpleServer", "route = $route")
//                    ctx.runOnUiThread {
//                        toast(route)
//                    }
                    break
                }
            } while (!line.isEmpty())

            // Output stream that we send the response to
            output = PrintStream(socket.getOutputStream())

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
                println("Content-Type: " + detectMimeType(".jpg"))
                println("Content-Length: " + bytes.size)
                println()
                write(bytes)
                flush()
            }
        } finally {
            if (null != output) output.close()
            reader?.close()
        }
    }

    @Throws
    private fun loadContent(route: String): ByteArray? {
        val (z, x, y) = route.split("/").subList(0, 3).map { it.toInt() }
//        ctx.runOnUiThread { toast("z, x, y = $z, $x, $y") }
        Log.d("simpleServer", "z, x, y = $z, $x, $y")

//        val input = ctx.assets.open(route)

        try {
            val output = ByteArrayOutputStream()
//            output.write(input.readBytes())
            val content = sources[0].getTile(z, x, (2.0.pow(z)).toInt() - 1 - y) ?: return null
            Log.d("simpleServer", "content exist")
            output.write(content)
            output.flush()
            return output.toByteArray()
        } catch (e: FileNotFoundException) {
        } finally {
//            input?.close()
        }
        return null
    }

    private fun writeServerError(output: PrintStream) {
        output.println("HTTP/1.0 500 Internal Server Error")
        output.flush()
    }

    private fun detectMimeType(fileName: String): String? = when {
        fileName.isEmpty() -> null
        fileName.endsWith(".jpg") -> "image/jpeg"
        fileName.endsWith(".png") -> "image/png"
        else -> "application/octet-stream"
    }
}