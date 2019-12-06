package com.simplemobiletools.contacts.pro.interfaces

import com.simplemobiletools.contacts.pro.models.Contact

interface RefreshContactsListener {
    fun refreshContacts(refreshTabsMask: Int)

    fun contactClicked(contact: Contact)
}
