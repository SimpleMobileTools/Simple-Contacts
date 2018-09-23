package com.simplemobiletools.contacts.activities

import android.os.Bundle
import android.text.SpannableStringBuilder
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.updateTextColors
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
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_wrapper)
        dialpad_clear_char.applyColorFilter(config.textColor)
    }

    private fun dialpadPressed(char: String) {
        dialpad_input.append(char)
    }

    private fun clearChar() {
        val curPostion = dialpad_input.selectionEnd
        val reducedPos = Math.max(curPostion - 1, 0)
        val selectedString = SpannableStringBuilder(dialpad_input.text)
        selectedString.replace(reducedPos, curPostion, "")
        dialpad_input.text = selectedString
        dialpad_input.setSelection(reducedPos)
    }

    private fun clearInput() {
        dialpad_input.setText("")
    }
}
