package com.simplemobiletools.contacts.pro.interfaces

import com.simplemobiletools.contacts.pro.models.Contact

interface RemoveFromGroupListener {
    fun removeFromGroup(contacts: ArrayList<Contact>)
}
