package com.simplemobiletools.contacts.models

import com.simplemobiletools.commons.helpers.SORT_BY_NUMBER
import com.simplemobiletools.commons.helpers.SORT_DESCENDING

data class Contact(val id: Int, var firstName: String, var middleName: String, var surname: String, var photoUri: String, var number: String,
                   var email: String, var source: String) : Comparable<Contact> {
    companion object {
        var sorting: Int = 0
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            (sorting and SORT_BY_NUMBER != 0) -> number.toLowerCase().compareTo(other.number.toLowerCase())
            else -> if (firstName.firstOrNull()?.isLetter() == true && other.firstName.firstOrNull()?.isLetter() == false) {
                -1
            } else if (firstName.firstOrNull()?.isLetter() == false && other.firstName.firstOrNull()?.isLetter() == true) {
                1
            } else {
                firstName.toLowerCase().compareTo(other.firstName.toLowerCase())
            }
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    fun getBubbleText() = when {
        sorting and SORT_BY_NUMBER != 0 -> number
        else -> firstName
    }

    fun getFullName(): String {
        var name = firstName
        if (middleName.isNotEmpty()) {
            name += " $middleName"
        }
        return "$name $surname".trim()
    }
}
