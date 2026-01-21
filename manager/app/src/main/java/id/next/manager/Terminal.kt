package id.next.manager

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.FrameLayout.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.RelativeLayout
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.view.View.OnKeyListener
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.TableRow
import android.view.View.OnClickListener
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Scanner
import android.widget.Toast
import android.icu.util.Output
import java.io.DataOutputStream
import java.io.DataInputStream
import android.os.Handler
import android.os.Message
import android.content.Intent
import android.util.Log
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import android.view.View.OnLongClickListener
import java.io.*

class Terminal : Activity() {

    var pwd: String = "/sdcard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //requestWindowFeature(Window.FEATURE_NO_TITLE) //hide TitleBar
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) //resize layout after showing keyboard
        val command = EditText(this).apply {
            gravity = Gravity.NO_GRAVITY
            imeOptions = EditorInfo.IME_ACTION_NONE
            setRawInputType(InputType.TYPE_CLASS_TEXT)
            isSingleLine = false
            hint = "what do you think?!"
        }

        val execute = Button(this).apply {
            text = "-->"
        }

        val output = TextView(this).apply {
            text = "Hey Guys (^_^)/"
            setTextIsSelectable(true)
        }

        val canvas = RelativeLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(20, 20, 20, 20)
        }

        val layoutCommandInput = LinearLayout(this).apply {
            layoutParams = LayoutParams(800, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            addView(command)
        }

        val layoutCommandButton = LinearLayout(this).apply {
            layoutParams = LayoutParams(200, 500)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            addView(execute)
        }

        val layoutCommand = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            addView(layoutCommandInput)
            addView(layoutCommandButton)
        }

        val layoutOutput = LinearLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).also {
                it.setMargins(0, 0, 0, 120)
            }
            orientation = LinearLayout.VERTICAL
            addView(output)
        }

        canvas.addView(layoutOutput)
        canvas.addView(layoutCommand)
        setContentView(canvas)

        execute.setOnClickListener {
            val input = command.text.toString()
            run(input, output)
            command.setText(null)
        }

        execute.setOnLongClickListener {
            pwd = command.text.toString()
            Toast.makeText(application, "cd $pwd", Toast.LENGTH_SHORT).show()
            run("ls", output)
            command.setText("ls")
            false
        }
    }

    private fun run(input: String, output: TextView) {
    
    val out = "┌──(Next@Manager)-[$pwd]\n└─# "
    val ot2 = ":/# "
    output.text = out
    output.append("$input\n")
    val process = try {
        Runtime.getRuntime().exec("su")
    } catch (e: IOException) {
        null
    } ?: return

    val stdIn = DataOutputStream(process.outputStream)
    val writer = BufferedWriter(OutputStreamWriter(stdIn))
    val stdOut = DataInputStream(process.inputStream)
    val reader2 = BufferedReader(InputStreamReader(stdOut))

    try {
        writer.write("cd $pwd\n")
        writer.write(input)
        writer.close()
        var line: String?
        while (reader2.readLine().also { line = it } != null) {
            output.append("$line\n")
        }
    } catch (e: IOException) {
        // Handle exception if needed
    }
    }
    
}
