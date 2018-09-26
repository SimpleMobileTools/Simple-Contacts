package com.simplemobiletools.contacts.activities

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getColoredDrawableWithColor
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.KEY_PHONE
import kotlinx.android.synthetic.main.activity_insert_edit_contact.*

class InsertOrEditContactActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)
        title = getString(R.string.select_contact)
        setupViews()

        handlePermission(PERMISSION_READ_CONTACTS) {
            // we do not really care about the permission request result. Even if it was denied, load private contacts
        }
    }

    private fun setupViews() {
        updateTextColors(insert_edit_contact_holder)
        new_contact_tmb.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_new_contact, config.textColor))
        new_contact_holder.setOnClickListener {
            Intent().apply {
                action = Intent.ACTION_INSERT
                data = ContactsContract.Contacts.CONTENT_URI
                putExtra(KEY_PHONE, intent.getStringExtra(KEY_PHONE))
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }

        existing_contact_label.setTextColor(getAdjustedPrimaryColor())
    }
}
