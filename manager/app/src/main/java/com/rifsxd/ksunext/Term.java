package com.rifsxd.ksunext;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import java.io.IOException;
import android.view.inputmethod.EditorInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.util.TypedValue; 
import androidx.annotation.Keep;

public class Term extends Activity {

    private TermUI ui;
    private ThRE thre;
    private String files;
    private String PWD = "/sdcard";
    private String input;
    private boolean isEnterToSpace = false;
    public final Handler handle = new Handler(Looper.getMainLooper());
    private boolean isCdToggleOn = false; 

    private static final int PICK_APK_REQUEST = 101;
    private static final int PICK_ZIP_REQUEST = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Inisialisasi UI
        ui = new TermUI(this);
        setContentView(ui.getRootView());

        files = getApplicationContext().getFilesDir().getAbsolutePath();

        thre = new ThRE(this);
        thre.start();

        initShell();
        setupListeners();
        ui.btnFull.performClick(); 
        isCdToggleOn = loadSyncPref();
        if (isCdToggleOn) {
            ui.tvPathDisplay.setText(PWD);
        }
    }

    private boolean loadSyncPref() {
        android.content.SharedPreferences pref = this.getSharedPreferences("TermPrefs", 0);
        return pref.getBoolean("auto_sync", false);
    }

    private void initShell() {
        thre.exec("su");
        thre.exec("export HOME=" + PWD);
        thre.exec("cd $HOME");
        thre.exec("export TBIN=" + files + "/bin");
        thre.exec("export FILES=" + files);
        thre.exec("export PATH=\"/product/bin:/apex/com.android.runtime/bin:/system/bin:/system/xbin:" + files + "/bin:$PATH\"");
        thre.exec("echo 'Hey Guys (^_^)/'");
        thre.exec("date");
    }

    private void setupListeners() {
        ui.btnSetting.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showSettingsDialog();
				}
			});

        ui.btnSwitch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					isEnterToSpace = !isEnterToSpace;
					ui.btnSwitch.setTextColor(isEnterToSpace ? 0xFF4285F4 : 0xFFE3E3E3);

					String currentText = ui.command.getText().toString();

					if (isEnterToSpace) {
						if (currentText.contains("\n")) {
							String cleanText = currentText.trim()
								.replaceAll("(?m)^\\s*#.*$", "") 
								.replaceAll("\\n+", " ; ")
								.replaceAll(" +", " ")
								.replaceAll("; then ;", "; then")
								.replaceAll("; else ;", "; else")
								.replaceAll(";(\\s*;)+", ";");
							
							cleanText = cleanText.replaceAll("^\\s*;|;\\s*$", "").trim();

							ui.command.setText(cleanText);
							ui.command.setSelection(ui.command.getText().length());
						}
						ui.command.setSingleLine(true);
						ui.command.setImeOptions(EditorInfo.IME_ACTION_SEND);
					} else {
						if (currentText.contains(" ; ") || currentText.contains(";")) {
							String cleanText = currentText.replaceAll("\\s*;\\s*", "\n").trim();
							ui.command.setText(cleanText);
							ui.command.setSelection(ui.command.getText().length());
						}
						ui.command.setSingleLine(false);
						
						int maxHeightDp = 300; 
						int maxHeightPx = (int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 
							maxHeightDp, 
							ui.command.getContext().getResources().getDisplayMetrics()
							);
						ui.command.setMaxHeight(maxHeightPx); 
						ui.command.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
						ui.command.requestLayout();
						ui.command.setImeOptions(EditorInfo.IME_ACTION_NONE);
					}
					ui.command.setSelection(ui.command.getText().length());

					android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) v.getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.restartInput(ui.command);
					}

					showGeminiToast(isEnterToSpace ? "Mode: Send (Single Line)" : "Mode: Edit (Multi Line)");
				}
			});

        ui.btnFull.setOnClickListener(new View.OnClickListener() {
				boolean isNoTitle = false;

				@Override
				public void onClick(View v) {
					isNoTitle = !isNoTitle;
					android.app.ActionBar actionBar = getActionBar();

					if (actionBar != null) {
						if (isNoTitle) {
							actionBar.hide(); 
						} else {
							actionBar.show(); 
						}
					} else {
						showGeminiToast("Your theme already has no Title");
					}
				}
			});

        ui.command.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
					if (isEnterToSpace && actionId == EditorInfo.IME_ACTION_SEND) {
						ui.btnSend.performClick(); 
						return true;
					}
					return false;
				}
			});

        ui.command.addTextChangedListener(new android.text.TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
					if (isEnterToSpace && count > 0) {
						String newChar = s.subSequence(start, start + count).toString();
						if (newChar.equals("\n")) {
							ui.command.getText().replace(start, start + count, "");
							ui.btnSend.performClick();
						}
					}
				}
				@Override public void afterTextChanged(android.text.Editable s) {}
			});

        ui.tvPathDisplay.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					isCdToggleOn = !isCdToggleOn; 

					if (isCdToggleOn) {
						ui.tvPathDisplay.setText(PWD);
						ui.tvPathDisplay.setAlpha(1.0f);
					} else {
						ui.tvPathDisplay.setText(null);
						ui.tvPathDisplay.setAlpha(0.4f);
					}
				}
			});

        ui.tvPathDisplay.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					String currentPath = ui.tvPathDisplay.getText().toString();
					if (!currentPath.isEmpty()) {
						try {
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
								v.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText("TermPath", currentPath);
							clipboard.setPrimaryClip(clip);
							showGeminiToast("Copied: " + currentPath);
						} catch (Exception e) {
							showGeminiToast("Failed to copy path");
						}
					}
					return true;
				}
			});

        ui.btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					input = ui.command.getText().toString().trim();

					String replaceInput = input.replace("\n", " ; ");
					ui.output.setText(getPrompt(replaceInput + "\n"));

					if (input.equals("su")) {
						thre.exec("echo \"$(su -v) ($(su -V))\"");
					} else if (input.equals(".")) { 
						thre.exec("cd $(pwd -P)");
						thre.exec("pwd > " + files + "/PWD ; echo '[DONE]'");
						ui.command.setText(null);
						return;
					} else if (input.equals("exit")) {
						if (thre != null) {
							thre.shutdown();
						}
						showGeminiToast("See you again....");
						finish(); 
						return; 
					} else if (input.matches(".*\\bcd\\b.*") && isCdToggleOn) {
						thre.exec(input);
						thre.exec("pwd > " + files + "/PWD ; echo '[DONE]'");
					} else {
						thre.exec(input);
					}

					ui.command.setText(null);
				}
			});

        ui.btnSend.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					FE();
					return true;
				}
			});
    }

    private void FE() {
        FE_Menu assets = new FE_Menu(this);
        assets.extractAssets();
        if (thre != null) {
            thre.exec("chmod 777 " + files + "/bin/*");
        }
        showGeminiToast("Enjoy Your Life....");
    }

    public void showNextPrompt() {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						File pwdFile = new File(files, "PWD");
						if (pwdFile.exists()) {
							BufferedReader br = new BufferedReader(new FileReader(pwdFile));
							String line = br.readLine();
							if (line != null) PWD = line;
							br.close();
						}
					} catch (Exception e) {}
					String cleanInput = (input != null) ? input.replace("\n", " | ") : "";
					ui.output.setText(getPrompt(cleanInput + "\n"));
					ui.tvPathDisplay.setText(PWD);
				}
			});
    }

    private SpannableString getPrompt(String userCommand) {
        String nextPrompt = "┌──(" + Build.DEVICE + ")—[" + PWD + "]\n└─# " + (userCommand != null ? userCommand : "");
        SpannableString ss = new SpannableString(nextPrompt);
        try {
            int idxStart = nextPrompt.indexOf("┌──(");
            if (idxStart != -1) ss.setSpan(new ForegroundColorSpan(Color.BLUE), idxStart, idxStart + 4, 0);

            int idxDevice = nextPrompt.indexOf(Build.DEVICE);
            if (idxDevice != -1) ss.setSpan(new ForegroundColorSpan(Color.RED), idxDevice, idxDevice + Build.DEVICE.length(), 0);

            int idxSep = nextPrompt.indexOf(")—[");
            if (idxSep != -1) ss.setSpan(new ForegroundColorSpan(Color.BLUE), idxSep, idxSep + 3, 0);

            int idxEndPath = nextPrompt.indexOf("]");
            if (idxEndPath != -1) ss.setSpan(new ForegroundColorSpan(Color.BLUE), idxEndPath, idxEndPath + 1, 0);

            int idxLine2 = nextPrompt.indexOf("└─");
            if (idxLine2 != -1) ss.setSpan(new ForegroundColorSpan(Color.BLUE), idxLine2, idxLine2 + 2, 0);

            int idxHash = nextPrompt.indexOf("#");
            if (idxHash != -1) ss.setSpan(new ForegroundColorSpan(Color.RED), idxHash, idxHash + 1, 0);
        } catch (Exception e) {}
        return ss;
    }

    private void showGeminiToast(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(14);
        tv.setPadding(60, 24, 60, 24); 
        tv.setTypeface(android.graphics.Typeface.SANS_SERIF);

        int bgColor, txtColor;
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorBackgroundFloating, typedValue, true);
            bgColor = typedValue.data;
            getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
            txtColor = typedValue.data;
        } catch (Exception e) {
            bgColor = 0xCC121212; 
            txtColor = 0xFFE3E3E3;
        }

        tv.setTextColor(txtColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(100f);
        bg.setColor(bgColor);
        bg.setStroke(1, 0x33888888); 
        tv.setBackground(bg);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 200);
        toast.setView(tv);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (thre != null) {
            thre.shutdown();
        }
    }

    public TermUI getUI() {
        return ui;
    }

    private void saveSyncPref(boolean value) {
        android.content.SharedPreferences pref = this.getSharedPreferences("TermPrefs", 0);
        pref.edit().putBoolean("auto_sync", value).apply();
    }

    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("FE Menu");

        String[] options = {
            "•➤ Auto-Sync Path" + (isCdToggleOn ? " | on" : " | off"),
            "•➤ Change Theme",
            "•➤ Clear Cache",
            "•➤ Apk Installer",
            "•➤ KaliFS Installer",
            "•➤ Delete Bin File"
        };

        builder.setItems(options, new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					switch (which) {
						case 0: // Toggle Auto-Sync
							isCdToggleOn = !isCdToggleOn;
							saveSyncPref(isCdToggleOn);
							ui.tvPathDisplay.setAlpha(isCdToggleOn ? 1.0f : 0.4f);
							showGeminiToast("Auto-Sync: " + (isCdToggleOn ? "ON" : "OFF"));
							showSettingsDialog(); 
							break;
						case 1: // Change Theme
							showGeminiToast("Feature not added yet!");
							break;
						case 2: // Clear Cache
							if (thre != null) {
								showGeminiToast("Cleaning cache, please wait...");
								thre.exec("su -c 'sh " + files + "/bin/cc'");
							}
							dialog.dismiss();
							break;
						case 3: // Apk Installer
							openApkPicker();
							dialog.dismiss();
							break;
						case 4: // KaliFS Installer
							pickNhZipFile();
							dialog.dismiss();
							break;
						case 5: // Delete Bin File Logic
							showDeleteBinMenu();
							dialog.dismiss();
							break;
					}
				}
			});

        builder.setPositiveButton("Exit App", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					finish();
				}
			});

        builder.setNeutralButton("About", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					showAboutDialog();
				}
			});

        builder.setNegativeButton("Minimize", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					android.content.Intent startMain = new android.content.Intent(android.content.Intent.ACTION_MAIN);
					startMain.addCategory(android.content.Intent.CATEGORY_HOME);
					startMain.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startMain);
					showGeminiToast("Running in background");
				}
			});

        builder.show();
    }

    private void showAboutDialog() {
        android.app.AlertDialog.Builder about = new android.app.AlertDialog.Builder(this);
        about.setTitle("About Lite Term");
        about.setMessage("Developer: ThRE Team\n" + "Build: v1.3.4 Beta\n");
        about.setPositiveButton("Close", null);
        about.show();
    }

    private void showDeleteBinMenu() {
        File binDir = new File(files, "bin");
        final File[] listFiles = binDir.listFiles();

        if (listFiles == null || listFiles.length == 0) {
            showGeminiToast("FE bin not installed");
            return;
        }

        final String[] fileNames = new String[listFiles.length];
        for (int i = 0; i < listFiles.length; i++) {
            fileNames[i] = listFiles[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove FE bin:");
        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					File selectedFile = listFiles[which];
					confirmDelete(selectedFile);
				}
			});
        builder.show();
    }

    private void confirmDelete(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Delete?");
        builder.setMessage("File: " + file.getName() + "\nThis file will be permanently deleted.");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteBinFile(file.getAbsolutePath(), file.getName());
					showDeleteBinMenu();
				}
			});
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void deleteBinFile(String fullPath, String name) {
        String cmd = "rm -rf \"" + fullPath + "\"";
        if (thre != null) {
            thre.exec(cmd);
            showGeminiToast("File " + name + " successfully deleted!");
        }
    }
    private void openApkPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Choose APK File"), PICK_APK_REQUEST);
    }

    private void pickNhZipFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip"); 
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Choose NetHunter ZIP File"), PICK_ZIP_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            String realPath = getRealPathFromURI(uri);

            if (requestCode == PICK_APK_REQUEST) {
                runApkInstaller(realPath);
            } else if (requestCode == PICK_ZIP_REQUEST) {
                showGeminiToast("Starting NH Installer with ZIP file...");
                runNhInstaller(realPath);
            }
        }
    }

    private void runNhInstaller(String zipPath) {
        String shellCmd = "su -c 'sh " + files + "/bin/install-kalifs.sh \"" + zipPath + "\"'";
        if (thre != null) {
            thre.exec(shellCmd);
        } else {
            showGeminiToast("ThRE is not ready yet!");
        }
    }

    private void runApkInstaller(String apkPath) {
        String binPath = files + "/bin/apk";
        String shellCmd = "su -c \"sh " + binPath + " '" + apkPath + "'\"";
        if (thre != null) {
            thre.exec(shellCmd);
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = "";
        try {
            if (uri.getPath().contains("primary:")) {
                filePath = "/sdcard/" + uri.getPath().split("primary:")[1];
            } else {
                String wholeID = uri.getPath(); 
                String id = wholeID.substring(wholeID.lastIndexOf(":") + 1);
                filePath = "/sdcard/Download/" + id; 

                File file = new File(filePath);
                if (!file.exists()) {
                    File tempFile = new File(getFilesDir(), "temp_install.apk");
                    java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                        outputStream.close();
                        inputStream.close();
                        filePath = tempFile.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            filePath = uri.getPath(); 
        }
        return filePath;
    }
}

