package com.simplemobiletools.contacts.pro.interfaces

import com.simplemobiletools.commons.models.contacts.Contact

interface RemoveFromGroupListener {
    fun removeFromGroup(contacts: ArrayList<Contact>)
}
