package com.simplemobiletools.contacts.models

data class Group(var id: Long, var title: String, var contactsCount: Int = 0) {
    fun addContact() = contactsCount++

    fun getBubbleText() = title
}
