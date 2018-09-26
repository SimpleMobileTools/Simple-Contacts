package com.simplemobiletools.contacts.activities

import android.os.Bundle
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.contacts.R

class InsertOrEditContactActivity : ContactActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)

        handlePermission(PERMISSION_READ_CONTACTS) {
            // we do not really care about the permission request result. Even if it was denied, load private contacts

        }
    }
}
