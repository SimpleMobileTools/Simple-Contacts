package com.simplemobiletools.contacts.pro.models

import com.simplemobiletools.contacts.pro.helpers.FIRST_GROUP_ID
import java.io.Serializable

data class Group(var id: Long, var title: String, var contactsCount: Int = 0) : Serializable {
    companion object {
        private const val serialVersionUID = -1384515348451345L
    }

    fun addContact() = contactsCount++

    fun getBubbleText() = title

    fun isPrivateSecretGroup() = id >= FIRST_GROUP_ID
}
