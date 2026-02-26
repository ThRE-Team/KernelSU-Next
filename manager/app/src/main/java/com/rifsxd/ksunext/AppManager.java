package id.thre.term;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import android.view.MenuItem;

public class AppManager {
    private Context context;
    private PackageManager pm;

    public AppManager(Context context) {
        this.context = context;
        this.pm = context.getPackageManager();
    }

    public void show() {
		final List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA | 512);
		final List<ApplicationInfo> filteredApps = new ArrayList<ApplicationInfo>();

		for (ApplicationInfo a : allApps) {
			if (pm.getLaunchIntentForPackage(a.packageName) != null || !a.enabled) {
				filteredApps.add(a);
			}
			/*
			 boolean isSystem = (a.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
			 if (pm.getLaunchIntentForPackage(a.packageName) != null || !a.enabled || isSystem) {
			 filteredApps.add(a);
			 }
			 */
		}

		java.util.Collections.sort(filteredApps, new java.util.Comparator<ApplicationInfo>() {
				@Override
				public int compare(ApplicationInfo app1, ApplicationInfo app2) {
					String label1 = app1.loadLabel(pm).toString();
					String label2 = app2.loadLabel(pm).toString();
					return label1.compareToIgnoreCase(label2);
				}
			});

		ListView lv = new ListView(context);
		lv.setDivider(null);
		lv.setPadding(10, 10, 10, 10);
		lv.setAdapter(new AppAdapter(filteredApps));

		final AlertDialog dialog = new AlertDialog.Builder(context)
			.setTitle("App Manager")
			.setView(lv)
			.create();

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					String pkg = filteredApps.get(position).packageName;
					context.startActivity(pm.getLaunchIntentForPackage(pkg));
					dialog.dismiss();
				}
			});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					String pkg = filteredApps.get(position).packageName;
					showOptions(pkg, view);
					return true;
				}
			});

		dialog.show();
	}

	private void showOptions(final String pkg, View anchor) {
		PopupMenu popup = new PopupMenu(context, anchor);

		boolean isEnabled = true;
		try {
			isEnabled = pm.getApplicationInfo(pkg, 0).enabled;
		} catch (Exception e) {}

		boolean isFrozen = false;
		if (android.os.Build.VERSION.SDK_INT >= 24) {
			try { isFrozen = pm.isPackageSuspended(pkg); } catch (Exception e) {}
		}
		popup.getMenu().add("App Info");
		popup.getMenu().add("Backup APK");
		popup.getMenu().add("Uninstall");
		popup.getMenu().add(isEnabled ? "Disable" : "Enable");
		popup.getMenu().add(isFrozen ? "Unfreeze" : "Freeze");
		popup.getMenu().add("Clear Cache");
		popup.getMenu().add("Clear Data");
		popup.getMenu().add("Kill Process");
		popup.getMenu().add("Freeform");

		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					handleRootAction(item.getTitle().toString(), pkg);
					return true;
				}
			});
		popup.show();
	}


	private void handleRootAction(String action, String pkg) {
		String cmd = "";

		switch (action) {
			case "Backup APK":
				extractApk(pkg);
				return;

			case "Uninstall":
				context.startActivity(new android.content.Intent(android.content.Intent.ACTION_DELETE, android.net.Uri.parse("package:" + pkg)));
				return;

			case "App Info":
				android.content.Intent i = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				i.setData(android.net.Uri.parse("package:" + pkg));
				context.startActivity(i);
				return;

			case "Disable":
				cmd = "pm disable " + pkg;
				break;

			case "Enable":
				cmd = "pm enable " + pkg;
				break;

			case "Freeze":
				cmd = "pm suspend " + pkg;
				break;

			case "Unfreeze":
				cmd = "pm unsuspend " + pkg;
				break;

			case "Clear Cache":
				cmd = "pm trim-caches 999G";
				break;

			case "Clear Data":
				cmd = "pm clear " + pkg;
				break;

			case "Kill Process":
				cmd = "am force-stop " + pkg;
				break;

			case "Freeform":
				android.content.Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
				if (launchIntent != null) {
					String component = launchIntent.getComponent().flattenToString();
					cmd = "am start -n " + component + " --windowingMode 5";
				}
				break;

			default:
				return;
		}

		if (!cmd.isEmpty()) {
			try {
				java.lang.Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
				//show();
				android.widget.Toast.makeText(context, "Success: " + action, 0).show();
			} catch (java.io.IOException e) {
				android.widget.Toast.makeText(context, "Root Error: " + e.getMessage(), 0).show();
			}
		}
	}

    private class AppAdapter extends BaseAdapter {
        private List<ApplicationInfo> list;

        public AppAdapter(List<ApplicationInfo> list) { this.list = list; }
        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int i) { return list.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
		public View getView(int i, View view, ViewGroup parent) {
			LinearLayout card = new LinearLayout(context);
			card.setOrientation(LinearLayout.HORIZONTAL);
			card.setGravity(android.view.Gravity.CENTER_VERTICAL); // Ikon & Teks sejajar tengah
			card.setPadding(30, 30, 30, 30);

			LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			cardParams.setMargins(15, 10, 15, 10);
			card.setLayoutParams(cardParams);

			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setCornerRadius(45f);
			gd.setStroke(2, android.graphics.Color.LTGRAY);
			card.setBackgroundDrawable(gd);

			ImageView icon = new ImageView(context);
			LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(130, 130);
			icon.setLayoutParams(iconParams);
			icon.setImageDrawable(list.get(i).loadIcon(pm));
			card.addView(icon);

			LinearLayout textLayer = new LinearLayout(context);
			textLayer.setOrientation(LinearLayout.VERTICAL);
			textLayer.setPadding(35, 0, 10, 0);
			LinearLayout.LayoutParams layerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
			textLayer.setLayoutParams(layerParams);

			TextView name = new TextView(context);
			name.setText(list.get(i).loadLabel(pm));
			//name.setTextColor(android.graphics.Color.BLACK);
			name.setTextSize(16f);
			name.setTypeface(null, android.graphics.Typeface.BOLD);
			name.setSingleLine(true);
			name.setEllipsize(android.text.TextUtils.TruncateAt.END);
			textLayer.addView(name);

			TextView pkg = new TextView(context);
			pkg.setText(list.get(i).packageName);
			//pkg.setTextColor(android.graphics.Color.GRAY);
			pkg.setTextSize(12f);
			pkg.setSingleLine(true);
			pkg.setEllipsize(android.text.TextUtils.TruncateAt.END);
			textLayer.addView(pkg);

			TextView sizeTxt = new TextView(context);
			sizeTxt.setText("Size: " + getAppSize(list.get(i).sourceDir));
			sizeTxt.setTextColor(android.graphics.Color.parseColor("#757575")); // Abu-abu gelap
			sizeTxt.setTextSize(11f);
			sizeTxt.setPadding(0, 5, 0, 0);
			textLayer.addView(sizeTxt);

			boolean isSystemApp = (list.get(i).flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
			String tagLabel = isSystemApp ? "SYSTEM" : "USER";
			int tagColor = isSystemApp ? android.graphics.Color.parseColor("#D32F2F") : android.graphics.Color.parseColor("#1976D2"); // Merah untuk System, Biru untuk User
			int bgColor = isSystemApp ? android.graphics.Color.parseColor("#FFEBEE") : android.graphics.Color.parseColor("#E3F2FD");
			TextView rootTag = new TextView(context);

			android.content.pm.ApplicationInfo info = list.get(i);
			StringBuilder statusBuilder = new StringBuilder();
			if (!info.enabled) {
				statusBuilder.append(" | Disabled");
			} 
			if ((info.flags & 0x40000000) != 0) { 
				statusBuilder.append(" | Frozen");
			}
			String status = statusBuilder.toString();


			rootTag.setText(tagLabel + status);

			rootTag.setTextSize(10f);
			rootTag.setTextColor(tagColor);
			//rootTag.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
			rootTag.setPadding(12, 4, 12, 4);

			/*android.graphics.drawable.GradientDrawable bgBadge = new android.graphics.drawable.GradientDrawable();
			 bgBadge.setColor(android.graphics.Color.parseColor("#E8F5E9"));
			 bgBadge.setCornerRadius(10f);
			 rootTag.setBackgroundDrawable(bgBadge);*/
			android.graphics.drawable.GradientDrawable bgBadge = new android.graphics.drawable.GradientDrawable();
			bgBadge.setColor(bgColor); // Warna background dinamis
			bgBadge.setCornerRadius(25f);
			rootTag.setBackgroundDrawable(bgBadge);
			LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			tagParams.setMargins(0, 8, 0, 0);
			rootTag.setLayoutParams(tagParams);

			textLayer.addView(rootTag);

			card.addView(textLayer);

			return card;
		}

    }

	private String getAppSize(String sourceDir) {
		java.io.File file = new java.io.File(sourceDir);
		if (!file.exists()) return "0 KB";
		long bytes = file.length();
		if (bytes < 1024) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String pre = "KMGTPE".charAt(exp - 1) + "";
		return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
	}

	private void extractApk(String pkgName) {
		try {
			android.content.pm.PackageManager pm = context.getPackageManager();
			android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(pkgName, 0);
			android.content.pm.PackageInfo pInfo = pm.getPackageInfo(pkgName, 0);

			String srcDirPath = new java.io.File(appInfo.sourceDir).getParent();
			String destDir = "/sdcard/Alarms/Backup";
			String safeLabel = appInfo.loadLabel(pm).toString()
				.replaceAll("[/\\\\:*?\"<>|]", "")
				.replace(" ", "_");
			String version = pInfo.versionName.replaceAll("[^0-9.]", "");
			String tarPath = destDir + "/" + safeLabel + "_v" + version + ".tar";
			String fullCmd = "su -c \"mkdir -p " + destDir + " && cd " + srcDirPath + " && " +
				"tar -cvf " + tarPath + " ./*.apk && " +
				"chmod 777 " + tarPath + "\"";

			java.lang.Runtime.getRuntime().exec(new String[]{"su", "-c", fullCmd});
			String msg = "Backup " + safeLabel + "_v" + version + ".apk\n" + tarPath;
			android.widget.Toast.makeText(context, msg, 1).show();
		} catch (Exception e) {
			android.widget.Toast.makeText(context, "Eror: " + e.getMessage(), 1).show();
		}
	}

}

