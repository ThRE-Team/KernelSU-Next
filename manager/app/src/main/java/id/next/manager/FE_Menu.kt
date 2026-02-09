package id.next.manager

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

internal class FE_Menu(private val context: Context) {

companion object {
    private const val TAG = "FE_Menu"
    fun getFilesDir(c: Context): String = c.filesDir.absolutePath
}

/**
 * Extract a single asset (given by path relative to rootAsset) into the app files dir.
 * Returns true on success, false on failure.
 */
private fun extractFile(rootAsset: String, path: String): Boolean {
    val assetManager: AssetManager = context.assets
    val assetPath = rootAsset + if (path.isEmpty()) "" else "/$path"
    val outFile = File(getFilesDir(context), path)

    // Ensure parent directories exist
    val parent = outFile.parentFile
    if (parent != null && !parent.exists()) {
        if (!parent.mkdirs()) {
            Log.w(TAG, "Failed to create parent dirs for ${outFile.absolutePath}")
            // continue and attempt to write anyway; it will fail if truly non-writable
        }
    }

    return try {
        assetManager.open(assetPath).use { input ->
            FileOutputStream(outFile).use { out ->
                val buffer = ByteArray(16 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.fd.sync()
                true
            }
        }
    } catch (e: IOException) {
        Log.w(TAG, "Failed to extract asset $assetPath to ${outFile.absolutePath}", e)
        false
    }
}

/**
 * Recursively extract an asset directory tree rooted at (rootAsset/path) into the app files dir.
 * Returns true if the whole subtree was extracted successfully.
 */
private fun extractDir(rootAsset: String, path: String): Boolean {
    val assetManager: AssetManager = context.assets
    val assetPath = rootAsset + if (path.isEmpty()) "" else "/$path"

    return try {
        val assets = assetManager.list(assetPath)
        if (assets == null) {
            // Treat as file if list() returned null
            return extractFile(rootAsset, path)
        }

        if (assets.isNotEmpty()) {
            // It's a directory in assets. Ensure directory exists in files dir.
            val dir = File(getFilesDir(context), path)
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create directory: ${dir.absolutePath}")
                // continue; files may fail to write later
            }

            for (asset in assets) {
                val childPath = if (path.isEmpty()) asset else "$path/$asset"
                if (!extractDir(rootAsset, childPath)) {
                    Log.e(TAG, "Failed to extract child: $childPath")
                    return false
                }
            }
        } else {
            // No children -> treat as file
            return extractFile(rootAsset, path)
        }
        true
    } catch (e: IOException) {
        Log.e(TAG, "Error listing assets for: $assetPath", e)
        false
    }
}

/**
 * Remove all files and subdirectories inside the given directory.
 * The directory itself is preserved.
 */
private fun cleanDirectory(path: File?) {
    if (path == null) return
    if (!path.exists()) return
    val list = path.listFiles() ?: return
    for (f in list) {
        if (f.isDirectory) {
            cleanDirectory(f)
            if (!f.delete()) {
                Log.w(TAG, "Failed to delete directory: ${f.absolutePath}")
            }
        } else {
            if (!f.delete()) {
                Log.w(TAG, "Failed to delete file: ${f.absolutePath}")
            }
        }
    }
}

/**
 * Recursively set permissive read/execute (and writable for owner) permissions.
 * Attempts to use Os.chmod when available; otherwise falls back to File.setXxx methods.
 */
private fun setPermissions(path: File?) {
    if (path == null) return
    if (!path.exists()) return

    // 0755 octal == 493 decimal
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        try {
            Os.chmod(path.absolutePath, 493)
        } catch (e: ErrnoException) {
            Log.w(TAG, "Os.chmod failed for ${path.absolutePath}, falling back to File#setXxx", e)
            path.setReadable(true, false)
            path.setExecutable(true, false)
            path.setWritable(true, true)
        }
    } else {
        path.setReadable(true, false)
        path.setExecutable(true, false)
        path.setWritable(true, true)
    }

    val list = path.listFiles() ?: return
    for (f in list) {
        if (f.isDirectory) {
            setPermissions(f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    Os.chmod(f.absolutePath, 493)
                } catch (ignored: ErrnoException) {
                    // ignore
                }
            } else {
                f.setReadable(true, false)
                f.setExecutable(true, false)
                f.setWritable(true, true)
            }
        } else {
            // 0644 octal == 420 decimal
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    Os.chmod(f.absolutePath, 420)
                } catch (ignored: ErrnoException) {
                    f.setReadable(true, false)
                    f.setWritable(true, true)
                }
            } else {
                f.setReadable(true, false)
                f.setWritable(true, true)
            }
        }
    }
}

/**
 * Extract the "ThRE" asset tree into the app files directory.
 * Returns true on full success, false if any file failed to extract.
 */
fun extractAssets(): Boolean {
    val ok = extractDir("ThRE", "")
    if (!ok) {
        Log.e(TAG, "extractDir failed for ThRE")
        return false
    }

    // Optionally enforce permissions on the extracted tree.
    val root = File(getFilesDir(context), "")
    setPermissions(root)
    return true
}

}