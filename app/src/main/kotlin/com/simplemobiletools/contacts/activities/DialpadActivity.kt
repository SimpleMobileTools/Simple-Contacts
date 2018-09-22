package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.R
import kotlinx.android.synthetic.main.activity_dialpad.*

class DialpadActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialpad)
        dialpad_0.setOnClickListener { }
        dialpad_1.setOnClickListener { }
        dialpad_2.setOnClickListener { }
        dialpad_3.setOnClickListener { }
        dialpad_4.setOnClickListener { }
        dialpad_5.setOnClickListener { }
        dialpad_6.setOnClickListener { }
        dialpad_7.setOnClickListener { }
        dialpad_8.setOnClickListener { }
        dialpad_9.setOnClickListener { }
        dialpad_asterisk.setOnClickListener { }
        dialpad_hashtag.setOnClickListener { }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialpad_wrapper)
    }
}
