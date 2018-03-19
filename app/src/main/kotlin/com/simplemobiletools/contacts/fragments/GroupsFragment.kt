package com.simplemobiletools.contacts.fragments

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.util.AttributeSet
import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.interfaces.FragmentInterface
import com.simplemobiletools.contacts.models.Contact

class GroupsFragment(context: Context, attributeSet: AttributeSet) : CoordinatorLayout(context, attributeSet), FragmentInterface {
    var activity: MainActivity? = null
    override fun setupFragment(activity: MainActivity) {
        this.activity = activity
    }

    override fun textColorChanged(color: Int) {
    }

    override fun primaryColorChanged(color: Int) {
    }

    override fun refreshContacts(contacts: ArrayList<Contact>) {
        if (activity == null) {
            return
        }

        val storedGroups = ContactsHelper(activity!!).getStoredGroups()
        contacts.forEach {
            it.groups.forEach {
                val group = it
                val storedGroup = storedGroups.firstOrNull { it.id == group.id }
                storedGroup?.addContact()
            }
        }
    }
}
