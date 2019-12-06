package com.simplemobiletools.contacts.pro.fragments

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.simplemobiletools.contacts.pro.activities.EditContactActivity
import com.simplemobiletools.contacts.pro.activities.MainActivity

class ContactsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun fabClicked() {
        Intent(context, EditContactActivity::class.java).apply {
            context.startActivity(this)
        }
    }

    override fun placeholderClicked() {
        (activity as? MainActivity)?.showFilterDialog()
    }

    override fun viewSetup() {}
}
