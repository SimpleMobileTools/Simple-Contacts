package com.simplemobiletools.contacts.pro.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_dialer.*

class DialerActivity : SimpleActivity() {
    private var number = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)

        if (intent.action == Intent.ACTION_CALL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            number = Uri.decode(intent.dataString).substringAfter("tel:")
            ContactsHelper(this).getContactWithNumber(number) {
                runOnUiThread {
                    updateCallee(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialer_holder)
    }

    private fun updateCallee(contact: Contact?) {
        if (contact != null) {
            callee_big_name_number.text = contact.getNameToDisplay()
            callee_number.text = number
        } else {
            callee_big_name_number.text = number
            callee_number.beGone()
        }
    }
}
