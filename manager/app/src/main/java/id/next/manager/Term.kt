package id.next.manager

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

class Term : Activity() {

private lateinit var thre: ThRE
private lateinit var execute: Button
private lateinit var output: TextView
private lateinit var command: EditText
private lateinit var canvas: RelativeLayout
private lateinit var layoutButton: LinearLayout
private lateinit var layoutCommandInput: LinearLayout
private lateinit var layoutCommandButton: LinearLayout
private lateinit var layoutCommand: LinearLayout
private lateinit var layoutOutput: LinearLayout

private lateinit var home: File
private lateinit var pwd: File
private var filesPath: String = ""
private var PWD: String = "/sdcard"

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // hide title bar
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    // resize layout when keyboard shown
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    // UI elements
    command = EditText(this).apply {
        gravity = Gravity.NO_GRAVITY
        imeOptions = EditorInfo.IME_ACTION_NONE
        setRawInputType(InputType.TYPE_CLASS_TEXT)
        isSingleLine = false
        hint = "what do you think?!"
    }

    execute = Button(this).apply { text = "-->" }

    output = TextView(this).apply {
        setTextIsSelectable(true)
    }

    canvas = RelativeLayout(this).apply {
        layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        setPadding(20, 20, 20, 20)
    }

    // command input area
    layoutCommandInput = LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM
        setPadding(0, 0, 120, 0)
        addView(command)
    }

    // command button area
    layoutCommandButton = LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(120, 120)
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM
        addView(execute)
    }

    // horizontal command container
    layoutCommand = LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM or Gravity.RIGHT
        addView(layoutCommandInput)
        // optional: addView(layoutCommandButton)
    }

    // right-side buttons container
    layoutButton = LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM or Gravity.RIGHT
        addView(layoutCommandButton)
    }

    // output container with bottom margin so input doesn't overlap
    layoutOutput = LinearLayout(this).apply {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        lp.setMargins(0, 0, 0, 120)
        layoutParams = lp
        orientation = LinearLayout.VERTICAL
        addView(output)
    }

    canvas.addView(layoutOutput)
    canvas.addView(layoutCommand)
    canvas.addView(layoutButton)
    setContentView(canvas)

    // file locations
    home = applicationContext.filesDir
    filesPath = home.absolutePath
    pwd = File(home, "PWD")
    PWD = "/sdcard"

    val externalFiles = getExternalFilesDir(null)
    val EXT = externalFiles?.absolutePath ?: ""

    // start ThRE thread (now non-null)
    thre = ThRE(this)
    thre.start()

    // Initial commands (ThRE.exec appends newline if needed)
    // Note: "su" only works on rooted devices
    thre.exec("su")
    thre.exec("export HOME=$PWD && cd \$HOME")
    thre.exec("export FILES=$filesPath")
    thre.exec("export TBIN=$filesPath/bin")
    thre.exec("export PATH=\"/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin:/system_ext/bin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin:/data/adb/ksu/bin:\$TBIN\"")
    thre.exec("echo 'Hey Guys (^_^)/'")
    thre.exec("date")
/*  if (pwd.exists()) {
        thre.exec("rm -f $filesPath/PWD")
    }
*/
    execute.setOnClickListener {
        val input = command.text?.toString() ?: ""
        val s = input.trim()
        val input = command.text?.toString() ?: ""
        val s = input.trim()
        val deviceInfo = "┌──(${Build.DEVICE})—[$PWD]\n└─# "
        // Creating a SpannableString object to allow setting specific text formatting
        val spannableText = SpannableString(deviceInfo)
        spannableText.setSpan(ForegroundColorSpan(Color.BLUE), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // "┌──("
        spannableText.setSpan(ForegroundColorSpan(Color.RED), 4, 4 + Build.DEVICE.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableText.setSpan(ForegroundColorSpan(Color.BLUE), deviceInfo.indexOf(")"), deviceInfo.indexOf(")") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // ")"
        spannableText.setSpan(ForegroundColorSpan(Color.BLUE), deviceInfo.indexOf("—["), deviceInfo.indexOf("—[") + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) // "-[ ]"
        spannableText.setSpan(ForegroundColorSpan(Color.BLUE), deviceInfo.indexOf("]"), deviceInfo.indexOf("]") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // "]"
        spannableText.setSpan(ForegroundColorSpan(Color.BLUE), deviceInfo.indexOf("└─"), deviceInfo.indexOf("└─") + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // "└─"
        spannableText.setSpan(ForegroundColorSpan(Color.RED), deviceInfo.indexOf("#"), deviceInfo.indexOf("#") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)  // "#"
        thre.exec(s)
        output.text = "spannableText"
        output.append("$s\n")
        command.setText(null)
    }
/*
        if (pwd.exists()) {
            try {
                BufferedReader(FileReader(pwd)).use { br ->
                    val l = br.readLine()
                    if (!l.isNullOrEmpty()) {
                        PWD = l
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // ignore empty commands
        if (s.isEmpty()) {
            command.setText(null)
            return@setOnClickListener
        }

        // basic safety: don't allow cd .. from filesystem root
        if (PWD == "/" && s == "cd ..") {
            command.setText(null)
            return@setOnClickListener
        }

        // exit: cleanup shell then finish activity
        if (s == "exit") {
            thre.shutdown()
            finish()
            return@setOnClickListener
        }

        // detect cd (cd, cd <path>)
        if (s == "cd" || s.startsWith("cd ") || s.contains(" cd") || s.contains(" cd ")  || s.contains("cd\n") || s.contains("\ncd")) {	
        //val cdRegex = Regex("""(?m)(?:^|\s)cd(?:\s|$)""")
        //if (cdRegex.containsMatchIn(s)) {
            // change dir and write PWD file so UI can pick it up
            val changeCmd = "$s && echo \$(pwd) > $filesPath/PWD"
            thre.exec(changeCmd)
            // optional: print a prompt via shell (escaped newline)
            thre.exec("echo \"┌──(Next@Manager)-[\$(cat $filesPath/PWD)]\\n└─# \"")
            output.text = null
        } else {
            // send command to shell (use trimmed command)
            thre.exec(s)
            output.text = "┌──(Next@Manager)-[$PWD]\n└─# "
            output.append("$s\n")
        }
        command.setText(null)
    }
*/
    execute.setOnLongClickListener {
        FE()
        true
    }
}

// Handler bound explicitly to main Looper to receive ThRE output
val handle: Handler = object : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
        output.append(msg.obj as String)
    }
}

private fun FE() {
    val assets = FE_Menu(this)
    assets.extractAssets()
    thre.exec("chmod 777 \$TBIN/*")
    Toast.makeText(applicationContext, "Enjoy Your Life....", Toast.LENGTH_SHORT).show()
}

override fun onDestroy() {
    super.onDestroy()
    thre.shutdown()
}

}