package com.rifsxd.ksunext;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.view.View.OnKeyListener;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.TableRow;
import android.view.View.OnClickListener;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import android.widget.Toast;
import android.icu.util.Output;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import android.view.View.OnLongClickListener;

public class Terminal extends Activity {

	Process process;
	DataOutputStream stdIn;
	DataInputStream stdOut;
	//DataInputStream stdErr;
	BufferedWriter writer;
	BufferedReader reader2;
	String pwd="/sdcard";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE); //hide TittleBar
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); //resize layout aftet showing-keyboard

		final EditText command = new EditText(this);
		command.setGravity(Gravity.NO_GRAVITY);
		command.setImeOptions(EditorInfo.IME_ACTION_NONE);
		command.setRawInputType(InputType.TYPE_CLASS_TEXT);
		command.setSingleLine(false);
		command.setHint("what do you think?!");

		final Button execute = new Button(this);
		execute.setText("-->");

		final TextView output = new TextView(this);
		output.setText("Hey Guys (^_^)/");
		output.setTextIsSelectable(true);

		RelativeLayout canvas = new RelativeLayout(this); //(weight, height);
		canvas.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		canvas.setPadding(20, 20, 20, 20);

		LinearLayout layoutCommandInput = new LinearLayout(this);
		layoutCommandInput.setLayoutParams(new LayoutParams(800, LayoutParams.MATCH_PARENT));
        layoutCommandInput.setOrientation(LinearLayout.VERTICAL);
		layoutCommandInput.setGravity(Gravity.BOTTOM);
		layoutCommandInput.addView(command);

		LinearLayout layoutCommandButton = new LinearLayout(this);
		layoutCommandButton.setLayoutParams(new LayoutParams(200, 500));
        layoutCommandButton.setOrientation(LinearLayout.VERTICAL);
		layoutCommandButton.setGravity(Gravity.BOTTOM);
		layoutCommandButton.addView(execute);

		LinearLayout layoutCommand = new LinearLayout(this);
		layoutCommand.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layoutCommand.setOrientation(LinearLayout.HORIZONTAL);
		layoutCommand.setGravity(Gravity.BOTTOM);
		layoutCommand.addView(layoutCommandInput);
		layoutCommand.addView(layoutCommandButton);

		LinearLayout layoutOutput = new LinearLayout(this);
		layoutOutput.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layoutOutput.setOrientation(LinearLayout.VERTICAL);
		LayoutParams margin = (LayoutParams) layoutOutput.getLayoutParams();
		margin.setMargins(0, 0, 0, 120); //setMargins(left, top, right, bottom);
		layoutOutput.setLayoutParams(margin);
		layoutOutput.addView(output);

		canvas.addView(layoutOutput);
		canvas.addView(layoutCommand);
        setContentView(canvas);
		// setContentView(R.layout.activity_main);

		execute.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					String input = command.getText().toString();
					run(input, output);
					command.setText(null);
				}

			}); //execute.setOnClickLimstener()

		execute.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View v) {

					pwd = command.getText().toString();
					Toast.makeText(getApplication(), "cd " + pwd , Toast.LENGTH_SHORT).show();
					run("ls", output);
					command.setText("ls");
					//command.setText(null);
					return false;
				}

			}); //execute.setOnLongClickListener

    } //onCreate
	private void run(String input, TextView output) {

		String out="┌──(Next@Manager)-[" + pwd + "]\n└─# ";
		String ot2=":/# ";
		//String input = command.getText().toString();
		output.setText(out);
		output.append(input + "\n");
		try {
			process = Runtime.getRuntime().exec("su");
		} catch (IOException e) {}
		stdIn = new DataOutputStream(process.getOutputStream());
		writer = new BufferedWriter(new OutputStreamWriter(stdIn));
		stdOut = new DataInputStream(process.getInputStream());
		reader2 = new BufferedReader(new InputStreamReader(stdOut));

		try {
			writer.write("cd " + pwd + "\n");
			writer.write(input);
			writer.close();
			String line;
			while ((line = reader2.readLine()) != null) {

				output.append(line + "\n");
				//reader2.close(); // dont close to read all
			}
		} catch (IOException e) {}

	}
} //main
