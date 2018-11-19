package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.activity_dialer.*

class DialerActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialer_holder)
    }
}
