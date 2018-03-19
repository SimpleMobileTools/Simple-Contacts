package com.simplemobiletools.contacts.models

import java.io.Serializable

data class Group(var id: Long, var title: String, var contactsCount: Int = 0) : Serializable {
    companion object {
        private const val serialVersionUID = -1384515348451345L
    }

    fun addContact() = contactsCount++

    fun getBubbleText() = title
}
