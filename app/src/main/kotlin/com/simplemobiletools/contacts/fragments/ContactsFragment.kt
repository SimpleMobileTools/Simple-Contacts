package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.ContactActivity

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        Intent(context, ContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun refreshItems() {
        initContacts()
    }
}
