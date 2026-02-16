package com.rifsxd.ksunext;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.annotation.Keep;

public class FE_Menu {

    private Context context;

    public FE_Menu(Context context) {
        this.context = context;
    }

    public boolean extractAssets() {
        File binDir = new File(context.getFilesDir(), "bin");
        cleanDirectory(binDir);
        boolean success = extractDir("ThRE", ""); 

        if (success) {
            setPermissions(binDir);
        }

        return success;
    }

    private boolean extractDir(String rootAsset, String path) {
        AssetManager assetManager = context.getAssets();
        try {
            String assetPath = (path.isEmpty()) ? rootAsset : rootAsset + path;
            String[] assets = assetManager.list(assetPath);

            if (assets != null && assets.length > 0) {
                String fullPath = context.getFilesDir().getAbsolutePath() + path;
                File dir = new File(fullPath);
                if (!dir.exists() && !dir.mkdirs()) return false;

                for (String asset : assets) {
                    if (!extractDir(rootAsset, path + "/" + asset)) return false;
                }
            } else {
                return extractFile(rootAsset, path);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean extractFile(String rootAsset, String path) {
        String assetPath = rootAsset + path;
        File outFile = new File(context.getFilesDir().getAbsolutePath() + path);

        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(assetPath);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
    }

    private void cleanDirectory(File path) {
        if (path == null || !path.exists()) return;
        File[] list = path.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) cleanDirectory(f);
                f.delete();
            }
        }
    }

    private void setPermissions(File path) {
        if (path == null || !path.exists()) return;
        try {
            Runtime.getRuntime().exec("chmod -R 755 " + path.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

