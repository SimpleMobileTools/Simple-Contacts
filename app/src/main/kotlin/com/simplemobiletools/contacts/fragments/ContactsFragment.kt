package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.EditContactActivity

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        Intent(context, EditContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun placeholderClicked() {
        activity!!.showFilterDialog()
    }
}
