package com.simplemobiletools.contacts.models

import android.graphics.Bitmap
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING

data class Contact(val id: Int, var firstName: String, var middleName: String, var surname: String, var photoUri: String,
                   var phoneNumbers: ArrayList<PhoneNumber>, var emails: ArrayList<Email>, var addresses: ArrayList<Address>, var events: ArrayList<Event>,
                   var source: String, var starred: Int, val contactId: Int, val thumbnailUri: String, var photo: Bitmap?, var notes: String,
                   var groups: ArrayList<Group>) : Comparable<Contact> {
    companion object {
        var sorting = 0
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> compareStrings(firstName, other.firstName)
            sorting and SORT_BY_MIDDLE_NAME != 0 -> compareStrings(middleName, other.middleName)
            else -> compareStrings(surname, other.surname)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    fun getBubbleText() = when {
        sorting and SORT_BY_FIRST_NAME != 0 -> firstName
        sorting and SORT_BY_MIDDLE_NAME != 0 -> middleName
        else -> surname
    }

    fun getFullName(startWithSurname: Boolean): String {
        var firstPart = if (startWithSurname) surname else firstName
        if (middleName.isNotEmpty()) {
            firstPart += " $middleName"
        }
        val lastPart = if (startWithSurname) firstName else surname
        return "$firstPart $lastPart".trim()
    }

    private fun compareStrings(first: String, second: String): Int {
        return if (first.firstOrNull()?.isLetter() == true && second.firstOrNull()?.isLetter() == false) {
            -1
        } else if (first.firstOrNull()?.isLetter() == false && second.firstOrNull()?.isLetter() == true) {
            1
        } else {
            if (first.isEmpty() && second.isNotEmpty()) {
                1
            } else if (first.isNotEmpty() && second.isEmpty()) {
                -1
            } else {
                first.toLowerCase().compareTo(second.toLowerCase())
            }
        }
    }
}
