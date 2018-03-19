package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.interfaces.FragmentInterface

class GroupsFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet), FragmentInterface {
    override fun setupFragment(activity: MainActivity) {
    }

    override fun textColorChanged(color: Int) {
    }

    override fun primaryColorChanged(color: Int) {
    }

    override fun refreshContacts() {
    }
}
