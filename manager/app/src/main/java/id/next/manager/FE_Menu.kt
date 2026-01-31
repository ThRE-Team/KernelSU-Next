package id.next.manager

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FE_Menu(private val context: Context) {

    companion object {
        fun getFilesDir(c: Context): String = c.filesDir.absolutePath
    }

    private fun close(c: AutoCloseable?) {
        try {
            c?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractFile(rootAsset: String, path: String) {
        val assetManager: AssetManager = context.assets
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = assetManager.open(rootAsset + path)
            val fullPath = getFilesDir(context) + path
            outputStream = FileOutputStream(fullPath)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
        } catch (e: IOException) {
            // ignore
        } finally {
            close(inputStream)
            close(outputStream)
        }
    }

    private fun extractDir(rootAsset: String, path: String): Boolean {
        val assetManager: AssetManager = context.assets
        try {
            val assets = assetManager.list(rootAsset + path) ?: return false
            if (assets.isNotEmpty()) {
                val fullPath = getFilesDir(context) + path
                val dir = File(fullPath)
                if (!dir.exists()) {
                    if (!dir.mkdir()) return false
                }
                for (asset in assets) {
                    if (!extractDir(rootAsset, "$path/$asset")) return false
                }
            } else {
                extractFile(rootAsset, path)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun cleanDirectory(path: File?) {
        if (path == null) return
        if (path.exists()) {
            val list = path.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) cleanDirectory(f)
                f.delete()
            }
        }
    }

    private fun setPermissions(path: File?) {
        if (path == null) return
        if (path.exists()) {
            path.setReadable(true, false)
            path.setExecutable(true, false)
            val list = path.listFiles() ?: return
            for (f in list) {
                if (f.isDirectory) setPermissions(f)
                f.setReadable(true, false)
                f.setExecutable(true, false)
            }
        }
    }

    fun extractAssets(): Boolean {
        extractDir("ThRE", "")
        return false
    }
}