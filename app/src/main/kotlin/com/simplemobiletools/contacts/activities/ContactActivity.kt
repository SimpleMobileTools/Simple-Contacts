package com.simplemobiletools.contacts.activities

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import kotlinx.android.synthetic.main.activity_contact.*

class ContactActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_cross)

        contact_photo.applyColorFilter(config.primaryColor.getContrastColor())
        contact_photo.background = ColorDrawable(config.primaryColor)

        val textColor = config.textColor
        contact_sms.applyColorFilter(textColor)
        contact_call.applyColorFilter(textColor)
        contact_email.applyColorFilter(textColor)
        contact_name_image.applyColorFilter(textColor)
        contact_number_image.applyColorFilter(textColor)
        contact_email_image.applyColorFilter(textColor)

        contact_photo.setOnClickListener { }
        contact_sms.setOnClickListener { }
        contact_call.setOnClickListener { }
        contact_email.setOnClickListener { }

        updateTextColors(contact_scrollview)
    }
}
