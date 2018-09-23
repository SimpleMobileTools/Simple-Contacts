package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.R
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
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_wrapper)
    }

    private fun dialpadPressed(char: String) {
        dialpad_input.append(char)
    }
}
