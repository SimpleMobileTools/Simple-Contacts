package com.simplemobiletools.contacts.activities

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.isLollipopPlus
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import kotlinx.android.synthetic.main.activity_dialpad.*

class DialpadActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialpad)

        dialpad_0.setOnClickListener { dialpadPressed("0") }
        dialpad_1.setOnClickListener { dialpadPressed("1") }
        dialpad_2.setOnClickListener { dialpadPressed("2") }
        dialpad_3.setOnClickListener { dialpadPressed("3") }
        dialpad_4.setOnClickListener { dialpadPressed("4") }
        dialpad_5.setOnClickListener { dialpadPressed("5") }
        dialpad_6.setOnClickListener { dialpadPressed("6") }
        dialpad_7.setOnClickListener { dialpadPressed("7") }
        dialpad_8.setOnClickListener { dialpadPressed("8") }
        dialpad_9.setOnClickListener { dialpadPressed("9") }
        dialpad_asterisk.setOnClickListener { dialpadPressed("*") }
        dialpad_hashtag.setOnClickListener { dialpadPressed("#") }
        dialpad_clear_char.setOnClickListener { clearChar() }
        dialpad_clear_char.setOnLongClickListener { clearInput(); true }
        disableKeyboardPopping()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_wrapper)
        dialpad_clear_char.applyColorFilter(config.textColor)
    }

    private fun dialpadPressed(char: String) {
        dialpad_input.dispatchKeyEvent(getKeyEvent(getCharKeyCode(char)))
    }

    private fun clearChar() {
        dialpad_input.dispatchKeyEvent(getKeyEvent(KeyEvent.KEYCODE_DEL))
    }

    private fun clearInput() {
        dialpad_input.setText("")
    }

    private fun getKeyEvent(keyCode: Int) = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0)

    private fun getCharKeyCode(char: String) = when (char) {
        "0" -> KeyEvent.KEYCODE_0
        "1" -> KeyEvent.KEYCODE_1
        "2" -> KeyEvent.KEYCODE_2
        "3" -> KeyEvent.KEYCODE_3
        "4" -> KeyEvent.KEYCODE_4
        "5" -> KeyEvent.KEYCODE_5
        "6" -> KeyEvent.KEYCODE_6
        "7" -> KeyEvent.KEYCODE_7
        "8" -> KeyEvent.KEYCODE_8
        "9" -> KeyEvent.KEYCODE_9
        "*" -> KeyEvent.KEYCODE_STAR
        else -> KeyEvent.KEYCODE_POUND
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun disableKeyboardPopping() {
        if (isLollipopPlus()) {
            dialpad_input.showSoftInputOnFocus = false
        } else {
            dialpad_input.inputType = InputType.TYPE_NULL
        }
    }
}
