package id.next.manager

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import android.os.Message;

class ThRE(private val term: Term) : Thread(), Runnable {

    @Volatile
    private var done = false
    private val stdIn: DataOutputStream
    private val stdOut: DataInputStream
    private val stdErr: DataInputStream
    private val process: Process

    init {
        try {
            process = Runtime.getRuntime().exec("/system/bin/sh")
            stdIn = DataOutputStream(process.outputStream)
            stdOut = DataInputStream(process.inputStream)
            stdErr = DataInputStream(process.errorStream)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun run() {
        try {
            Thread.sleep(100)
            while (!done) {
                while (stdOut.available() == 0 && stdErr.available() == 0) {
                    Thread.sleep(10)
                }
                dump()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun dump() {
        try {
            val out = StringBuilder()
            if (stdOut.available() > 0) {
                while (stdOut.available() > 0) {
                    out.append(stdOut.read().toChar())
                }
            }
            if (stdErr.available() > 0) {
                while (stdErr.available() > 0) {
                    out.append(stdErr.read().toChar())
                }
            }
            term.handle.sendMessage(Message.obtain(term.handle, 0, out.toString()))
        } catch (e: IOException) {
            // ignore
        }
    }

    fun exec(command: String) {
        try {
            stdIn.write(command.toByteArray())
            stdIn.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}