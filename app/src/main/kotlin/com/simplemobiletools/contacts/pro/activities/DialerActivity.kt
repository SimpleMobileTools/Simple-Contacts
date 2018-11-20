package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import kotlinx.android.synthetic.main.activity_dialer.*

class DialerActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)

        if (intent.action == Intent.ACTION_CALL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            val number = Uri.decode(intent.dataString).substringAfter("tel:")
            ContactsHelper(this).getContactWithNumber(number) {

            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialer_holder)
    }
}
