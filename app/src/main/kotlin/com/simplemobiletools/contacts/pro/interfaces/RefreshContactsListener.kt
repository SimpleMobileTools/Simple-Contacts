package com.simplemobiletools.contacts.pro.interfaces

import com.simplemobiletools.commons.models.contacts.*

interface RefreshContactsListener {
    fun refreshContacts(refreshTabsMask: Int)

    fun contactClicked(contact: Contact)
}
