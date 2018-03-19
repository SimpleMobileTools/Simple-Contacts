package com.simplemobiletools.contacts.interfaces

import com.simplemobiletools.contacts.activities.MainActivity
import com.simplemobiletools.contacts.models.Contact

interface FragmentInterface {
    fun setupFragment(activity: MainActivity)

    fun onActivityResume()

    fun textColorChanged(color: Int)

    fun primaryColorChanged(color: Int)

    fun refreshContacts(contacts: ArrayList<Contact>)

    fun showContactThumbnailsChanged(showThumbnails: Boolean)

    fun finishActMode()

    fun onSearchQueryChanged(text: String)

    fun onSearchOpened()

    fun onSearchClosed()
}
