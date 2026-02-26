package id.thre.term;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.TypedValue;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermUI {

    public Button btnSwitch, btnSetting, btnFull, btnSend;
    public EditText command;
    public TextView output, tvPathDisplay;
    public RelativeLayout canvas;
    private final Activity activity;

    private Term term; 
    private static final String PREFS_NAME = "ThRE_Session";
    private static final String KEY_HSTR = "history_data";

    public TermUI(Activity activity) {
        this.activity = activity;
        this.term = (Term) activity;
        initUI();
        initHistory();
    }

    private void initHistory() {
        SharedPreferences pref = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        String raw = pref.getString(KEY_HSTR, "");
        List<String> loaded = new ArrayList<String>();

        if (!raw.isEmpty()) {
            for (String s : raw.split(";;;")) {
                if (!s.trim().isEmpty()) loaded.add(s);
            }
        }
        term.setHistory(loaded);
    }

    private void saveToPref() {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE).edit();
        StringBuilder sb = new StringBuilder();
        for (String s : term.getHistory()) {
            sb.append(s).append(";;;");
        }
        editor.putString(KEY_HSTR, sb.toString());
        editor.apply();
    }

	public void showHistoryPopup() {
		final List<String> history = term.getHistory(); //
		if (history == null || history.isEmpty()) {
			term.showGeminiToast("not found!");
			return;
		}

		final List<String> displayList = new ArrayList<String>(history);
		Collections.reverse(displayList);
		final String[] items = displayList.toArray(new String[0]);

		android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
			activity, android.R.layout.simple_list_item_1, items) {
			@Override
			public View getView(int position, View convertView, android.view.ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				textView.setGravity(Gravity.CENTER);
				textView.setPadding(0, 20, 0, 20);
				return textView;
			}
		};

		android.widget.ListView listView = new android.widget.ListView(activity);
		listView.setAdapter(adapter);

		final AlertDialog dialog = new AlertDialog.Builder(activity)
			.setTitle("History Manager")
			.setView(listView)
			.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface d, int which) {
					term.getHistory().clear(); //
					saveToPref(); //
					term.showGeminiToast("History cleared!");
				}
			})
			.setNegativeButton("Cancel", null)
			.create();

		listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
					command.setText(items[position]); //
					command.setSelection(command.getText().length());
					dialog.dismiss();
				}
			});

		dialog.show();
	}
	

    public void handleCommand(String cmd) {
        if (cmd.trim().equalsIgnoreCase("hstr")) {
            showHistoryPopup();
            command.setText("");
        } else {
            term.addToHistory(cmd);
            saveToPref();
        }
    }

    private void initUI() {
        canvas = new RelativeLayout(activity);
        output = new TextView(activity);
        tvPathDisplay = new TextView(activity);
        command = new EditText(activity);

        btnSwitch = new Button(activity);
        btnSetting = new Button(activity);
        btnFull = new Button(activity); 
        btnSend = new Button(activity);

        setupSystemStyle();
        assembleGeminiLayout();

        btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handleCommand(command.getText().toString());
				}
			});
    }

    private void setupSystemStyle() {
        canvas.setBackgroundColor(0);
        canvas.setPadding(20, 20, 20, 20);

        output.setTextSize(13);
        output.setTextIsSelectable(true);

        tvPathDisplay.setTextSize(10);
        tvPathDisplay.setSingleLine(true);
        tvPathDisplay.setEllipsize(android.text.TextUtils.TruncateAt.START);
		tvPathDisplay.setClickable(true);
		tvPathDisplay.setFocusable(true);

        command.setBackgroundColor(0); 
        command.setHint("what do you think?!");
        command.setPadding(30, 20, 30, 10);
        command.setImeOptions(EditorInfo.IME_ACTION_NONE);
        command.setRawInputType(InputType.TYPE_CLASS_TEXT | 
                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | 
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        command.setSingleLine(false);

        btnSwitch.setText("⎈");
        btnSwitch.setBackgroundColor(0);
        btnSwitch.setTextSize(18);
        btnSwitch.setPadding(0, 0, 0, 0);
        btnSwitch.setGravity(Gravity.CENTER);

        btnSetting.setText("⚙");
        btnSetting.setBackgroundColor(0);
        btnSetting.setTextSize(18);
        btnSetting.setPadding(0, 0, 0, 0);
        btnSetting.setGravity(Gravity.CENTER);

        btnFull.setText("⛶"); 
        btnFull.setBackgroundColor(0);
        btnFull.setTextSize(18);
        btnFull.setPadding(0, 0, 0, 0);
        btnFull.setGravity(Gravity.CENTER);

        int sysColor;
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.colorSecondary, tv, true);
            sysColor = tv.data;
            activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        } catch (Exception e) {
            sysColor = 0x22888888;
        }

        btnSend.setText("••➤");
        btnSend.setAllCaps(false);
        btnSend.setTextSize(14);
        btnSend.setPadding(0, 0, 0, 0);
        btnSend.setGravity(Gravity.CENTER);
        btnSend.setIncludeFontPadding(false);

        GradientDrawable execShape = new GradientDrawable();
        execShape.setCornerRadius(100f);
		int sSysColor = (sysColor & 0x00000000) | 0x24000000;
		execShape.setColor(sSysColor); 
        btnSend.setBackground(execShape);

    }

	private void assembleGeminiLayout() {
        int sysColor;
        try {
            sysColor = activity.getWindow().getStatusBarColor();

            TypedValue tv = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true);
        } catch (Exception e) {
            sysColor = 0xFF888888;
        }

        int strokeColor = (sysColor & 0x00FFFFFF) | 0x44000000; 
        int inputBgColor = (sysColor & 0x00FFFFFF) | 0x15000000;

        LinearLayout layoutMainInput = new LinearLayout(activity);
        layoutMainInput.setOrientation(LinearLayout.VERTICAL);
        layoutMainInput.setId(View.generateViewId());

        GradientDrawable bgInput = new GradientDrawable();
        bgInput.setCornerRadius(60f);
        bgInput.setStroke(3, strokeColor);
        bgInput.setColor(inputBgColor);
        layoutMainInput.setBackground(bgInput);
        layoutMainInput.setPadding(5, 10, 5, 10);

        LinearLayout layoutActions = new LinearLayout(activity);
        layoutActions.setOrientation(LinearLayout.HORIZONTAL);
        layoutActions.setGravity(Gravity.CENTER_VERTICAL);
        layoutActions.setPadding(10, 0, 10, 0);

        LinearLayout.LayoutParams tightBtnLp = new LinearLayout.LayoutParams(95, 95);
        tightBtnLp.setMargins(0, 0, -15, 0); 
        layoutActions.addView(btnSwitch, tightBtnLp);
        layoutActions.addView(btnSetting, tightBtnLp);
        layoutActions.addView(btnFull, tightBtnLp);

        LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        pathLp.setMargins(25, 0, 10, 0); 
        layoutActions.addView(tvPathDisplay, pathLp);
        layoutActions.addView(btnSend, new LinearLayout.LayoutParams(140, 85));

        int maxHeightDp = 300; 
        int maxHeightPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            maxHeightDp, 
            activity.getResources().getDisplayMetrics()
        );
        command.setMaxHeight(maxHeightPx);
        layoutMainInput.addView(command, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lpActions = new LinearLayout.LayoutParams(-1, -2);
        lpActions.setMargins(0, 10, 0, 0); 
        layoutMainInput.addView(layoutActions, lpActions);
		
        RelativeLayout.LayoutParams inputLp = new RelativeLayout.LayoutParams(-1, -2);
        inputLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        inputLp.setMargins(5, 0, 5, 5); 
        layoutMainInput.setLayoutParams(inputLp);

        RelativeLayout.LayoutParams outLp = new RelativeLayout.LayoutParams(-1, -1);
        outLp.addRule(RelativeLayout.ABOVE, layoutMainInput.getId());
        outLp.setMargins(0, 0, 0, 20);

        LinearLayout layoutOutput = new LinearLayout(activity);
        layoutOutput.setLayoutParams(outLp);
        layoutOutput.setOrientation(LinearLayout.VERTICAL);
        layoutOutput.addView(output);

        canvas.addView(layoutOutput);
        canvas.addView(layoutMainInput);
    }

    public View getRootView() {
        return canvas;
    }
}

