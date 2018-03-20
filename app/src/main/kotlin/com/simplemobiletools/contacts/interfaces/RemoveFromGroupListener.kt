package com.simplemobiletools.contacts.interfaces

import com.simplemobiletools.contacts.models.Contact

interface RemoveFromGroupListener {
    fun removeFromGroup(contacts: ArrayList<Contact>)
}
