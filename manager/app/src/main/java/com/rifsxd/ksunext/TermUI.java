package com.rifsxd.ksunext;

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

public class TermUI {

    public Button btnSwitch, btnSetting, btnFull, btnSend;
    public EditText command;
    public TextView output, tvPathDisplay;
    public RelativeLayout canvas;
    private final Activity activity;

    public TermUI(Activity activity) {
        this.activity = activity;
        initUI();
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
    }

    private void setupSystemStyle() {
        canvas.setBackgroundColor(0);
        canvas.setPadding(20, 20, 20, 20);

        output.setTextSize(13);
        output.setTextIsSelectable(true);

        tvPathDisplay.setTextSize(10);
        tvPathDisplay.setSingleLine(true);
        tvPathDisplay.setEllipsize(android.text.TextUtils.TruncateAt.START);
		TypedValue outValue = new TypedValue();
		activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		tvPathDisplay.setBackgroundResource(outValue.resourceId);

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

        // Styling Tombol Switch (☯) - Tukar fungsi Enter
        btnSwitch.setText("☯");
        btnSwitch.setBackgroundColor(0);
        btnSwitch.setTextSize(18);
        btnSwitch.setPadding(0, 0, 0, 0);
        btnSwitch.setGravity(Gravity.CENTER);

        // Styling Tombol Setting (⚙)
        btnSetting.setText("⚙");
        btnSetting.setBackgroundColor(0);
        btnSetting.setTextSize(18);
        btnSetting.setPadding(0, 0, 0, 0);
        btnSetting.setGravity(Gravity.CENTER);

        // Styling Tombol Fullscreen/Title (⛶)
        btnFull.setText("⛶"); 
        btnFull.setBackgroundColor(0);
        btnFull.setTextSize(18);
        btnFull.setPadding(0, 0, 0, 0);
        btnFull.setGravity(Gravity.CENTER);
		
        int sysColor;
        int sysTextColor;
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.colorSecondary, tv, true);
            sysColor = tv.data;

            activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
            sysTextColor = tv.data;
        } catch (Exception e) {
            // Fallback jika gagal
            sysColor = 0x22888888;
            sysTextColor = 0xFFE3E3E3;
        }

        btnSend.setText("••➤");
        btnSend.setAllCaps(false);
        btnSend.setTextSize(14);
        btnSend.setTextColor(sysTextColor); // Teks ikut sistem

        // Perbaikan Posisi
        btnSend.setPadding(0, 0, 0, 0);
        btnSend.setGravity(Gravity.CENTER);
        btnSend.setIncludeFontPadding(false);

        GradientDrawable execShape = new GradientDrawable();
        execShape.setCornerRadius(100f);

        // Gunakan warna sistem dengan sedikit transparansi (0x66) 
        // agar tetap terlihat "kaca" (glassmorphism) ala Gemini
        int transparentSysColor = (sysColor & 0x00FFFFFF) | 0x66000000;
        //execShape.setColor(transparentSysColor); 
		int sSysColor = (sysColor & 0x00000000) | 0x24000000;
		execShape.setColor(sSysColor); 
        btnSend.setBackground(execShape);
		
		
    }

	private void assembleGeminiLayout() {
        // --- 1. AMBIL WARNA SISTEM (THEME AWARE) ---
        int sysColor;
        int sysTextColor;
        try {
            // Ambil warna Status Bar (biasanya warna dominan tema)
            sysColor = activity.getWindow().getStatusBarColor();

            // Ambil warna teks utama sistem
            TypedValue tv = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
            sysTextColor = tv.data;
        } catch (Exception e) {
            // Fallback kalau gagal ambil (Default ke Abu-abu & Putih)
            sysColor = 0xFF888888;
            sysTextColor = 0xFFE3E3E3;
        }

        // --- 2. RACIK WARNA TRANSPARAN (GLASSMORPHISM) ---
        // Logika: Ambil warna RGB-nya, buang Alpha lama, ganti Alpha baru
        // 0x44... = Transparansi Border (sedikit tebal)
        // 0x15... = Transparansi Isi Kotak (tipis banget)
        int strokeColor = (sysColor & 0x00FFFFFF) | 0x44000000; 
        int inputBgColor = (sysColor & 0x00FFFFFF) | 0x15000000; 
        int hintColor = (sysTextColor & 0x00FFFFFF) | 0x88000000; // Hint setengah transparan

        // --- 3. SETUP CONTAINER UTAMA ---
        LinearLayout layoutMainInput = new LinearLayout(activity);
        layoutMainInput.setOrientation(LinearLayout.VERTICAL);
        layoutMainInput.setId(View.generateViewId());

        GradientDrawable bgInput = new GradientDrawable();
        bgInput.setCornerRadius(60f); // Sudut membulat
        bgInput.setStroke(3, strokeColor); // Border ikut warna tema
        bgInput.setColor(inputBgColor);    // Isi ikut warna tema
        layoutMainInput.setBackground(bgInput);
        layoutMainInput.setPadding(5, 10, 5, 10);

        // --- 4. SETUP TOMBOL IKON (ACTIONS) ---
        LinearLayout layoutActions = new LinearLayout(activity);
        layoutActions.setOrientation(LinearLayout.HORIZONTAL);
        layoutActions.setGravity(Gravity.CENTER_VERTICAL);
        layoutActions.setPadding(10, 0, 10, 0);

        // Helper kecil biar rapi setting tombolnya
        // Kita set warna teks tombol ikon agar kontras
        btnSwitch.setTextColor(sysTextColor);
        btnSetting.setTextColor(sysTextColor);
        btnFull.setTextColor(sysTextColor);

        // Aturan Jarak Ikon (Rapat)
        LinearLayout.LayoutParams tightBtnLp = new LinearLayout.LayoutParams(95, 95);
        tightBtnLp.setMargins(0, 0, -15, 0); 

        layoutActions.addView(btnSwitch, tightBtnLp);
        layoutActions.addView(btnSetting, tightBtnLp);
        layoutActions.addView(btnFull, tightBtnLp);

        // --- 5. SETUP TEXTVIEW PATH ---
        // Teks PWD dengan Jarak 25px dari btnFull
        tvPathDisplay.setTextColor(sysTextColor); // Path ikut warna teks sistem
        LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
        pathLp.setMargins(25, 0, 10, 0); 
        layoutActions.addView(tvPathDisplay, pathLp);

        // Tombol Send (sudah di-handle di initUI, tinggal add)
        layoutActions.addView(btnSend, new LinearLayout.LayoutParams(140, 85));

        // --- 6. SETUP EDITTEXT (COMMAND) ---
        // Terapkan warna ke kolom ketik
        command.setTextColor(sysTextColor);
        command.setHintTextColor(hintColor); // Hint warnanya soft

        // Hitung tinggi maksimal (Logic yang sudah kita bahas sebelumnya)
        int maxHeightDp = 300; 
        int maxHeightPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            maxHeightDp, 
            activity.getResources().getDisplayMetrics()
        );
        command.setMaxHeight(maxHeightPx); 

        // Masukkan elemen ke Layout Utama
        layoutMainInput.addView(command, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lpActions = new LinearLayout.LayoutParams(-1, -2);
        lpActions.setMargins(0, 10, 0, 0); 
        layoutMainInput.addView(layoutActions, lpActions);

        // --- 7. FINALISASI LAYOUT KE CANVAS ---
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

