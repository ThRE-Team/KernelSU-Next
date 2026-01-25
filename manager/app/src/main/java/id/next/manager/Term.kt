package id.next.manager

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.RelativeLayout.LayoutParams
import android.view.View.OnLongClickListener
import android.view.View.OnClickListener
import android.view.Window

class Term : Activity() {

    companion object {
        const val bash = false
    }

    lateinit var process: Process
    lateinit var thre: ThRE
    lateinit var execute: Button
    lateinit var output: TextView
    lateinit var command: EditText
    lateinit var canvas: RelativeLayout
    lateinit var layoutButton: LinearLayout
    lateinit var layoutCommandInput: LinearLayout
    lateinit var layoutCommandButton: LinearLayout
    lateinit var layoutCommand: LinearLayout
    lateinit var layoutOutput: LinearLayout
    lateinit var margin: LayoutParams

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE) //hide TitleBar
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) //resize layout after showing keyboard

        command = EditText(this).apply {
            gravity = Gravity.NO_GRAVITY
            imeOptions = EditorInfo.IME_ACTION_NONE
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(false)
            hint = "what do you think?!"
        }

        execute = Button(this).apply {
            text = "-->"
        }

        output = TextView(this).apply {
            //text = "Hey Guys (^_^)/"
            setTextIsSelectable(true)
        }

        canvas = RelativeLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(20, 20, 20, 20)
        }

        layoutCommandInput = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(0, 0, 120, 0) //(left, top, right, bottom)
            addView(command)
        }

        layoutCommandButton = LinearLayout(this).apply {
            layoutParams = LayoutParams(120, 120)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            addView(execute)
        }

        layoutCommand = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            addView(layoutCommandInput)
            //addView(layoutCommandButton)
        }

        layoutButton = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            addView(layoutCommandButton)
        }

        layoutOutput = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
        }
        margin = layoutOutput.layoutParams as LayoutParams
        margin.setMargins(0, 0, 0, 120) // setMargins(left, top, right, bottom)
        layoutOutput.layoutParams = margin
        layoutOutput.addView(output)

        canvas.addView(layoutOutput)
        canvas.addView(layoutCommand)
        canvas.addView(layoutButton)
        setContentView(canvas)

        thre = ThRE(this)
        thre.start()
        thre.exec("su\n")
        thre.exec("export PATH='/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin:/data/adb/ksu/bin:/data/user/0/id.next.manager/files/bin'\n")
        thre.exec("export HOME='/sdcard'\n")
		thre.exec("cd\n");
		thre.exec("echo 'Hey Guys (^_^)/' \n");
		thre.exec("date \n");

        execute.setOnClickListener {
            val input = command.text.toString() + "\n"
            output.text = "┌──(Next@Manager)-[/sdcard]\n└─# "
            output.append(input)
            thre.exec(input)
            command.setText(null)
        }

        execute.setOnLongClickListener {
            FE()
            false
        }
    }

    val handle = object : Handler() {
        override fun handleMessage(msg: Message) {
            output.append(msg.obj as String)
        }
    }
    
    private fun FE() {
        val assets = FE_Menu(this)
        assets.extractAssets()
		thre.exec("chmod 777 /data/user/0/id.next.manager/files/bin/*\n")
        Toast.makeText(application, "Enjoy Your Life....", Toast.LENGTH_SHORT).show()
    }
}