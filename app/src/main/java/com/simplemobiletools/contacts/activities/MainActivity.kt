package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.extensions.appLaunched
import com.simplemobiletools.contacts.R

class MainActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()
    }
}
