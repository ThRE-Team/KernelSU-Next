package id.next.manager;

import android.os.Message
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ThRE(private val term: Term) : Thread() {
 @Volatile
private var done = false
private val process: Process?
private val stdIn: OutputStream?
private val stdOut: BufferedReader?
private var outReader: Thread? = null

init {
    var p: Process? = null
    var inStream: OutputStream? = null
    var outReaderBuf: BufferedReader? = null
    try {
        val pb = ProcessBuilder("/system/bin/sh")
        pb.redirectErrorStream(true)
        p = pb.start()
        inStream = p.outputStream
        outReaderBuf = BufferedReader(InputStreamReader(p.inputStream, StandardCharsets.UTF_8))
    } catch (e: IOException) {
        e.printStackTrace()
    }
    process = p
    stdIn = inStream
    stdOut = outReaderBuf
    // readers started in run()
}

private fun startReaders() {
    stdOut?.let { reader ->
        outReader = Thread {
            readLoop(reader)
        }
        outReader?.start()
    }
}

private fun readLoop(reader: BufferedReader) {
    try {
        val sb = StringBuilder()
        while (!done) {
            val ch = reader.read()
            if (ch == -1) break
            sb.append(ch.toChar())
            // flush on newline or when buffer grows to prevent large delays
            if (ch == '\n'.code || sb.length >= 1024) {
                val chunk = sb.toString()
                sb.setLength(0)
                postToHandler(chunk)
            }
        }
        // flush remaining
        if (sb.isNotEmpty()) {
            postToHandler(sb.toString())
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

private fun postToHandler(text: String) {
    val handler = term.handle
    handler.post {
        val msg = Message.obtain(handler, 0, text)
        handler.sendMessage(msg)
    }
}

override fun run() {
    startReaders()
    try {
        process?.waitFor()
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    } finally {
        shutdown()
    }
}

@Synchronized
fun exec(command: String) {
    try {
        if (stdIn != null && process != null && process.isAlive) {
            stdIn.write((command + "\n").toByteArray(StandardCharsets.UTF_8))
            stdIn.flush()
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

@Synchronized
fun shutdown() {
    done = true
    try {
        stdIn?.close()
    } catch (ignored: IOException) {}
    try {
        stdOut?.close()
    } catch (ignored: IOException) {}
    process?.destroy()
    outReader?.interrupt()
}

}