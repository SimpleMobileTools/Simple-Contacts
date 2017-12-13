package com.simplemobiletools.contacts.models

import com.simplemobiletools.commons.helpers.*

data class Contact(val id: Int, var firstName: String, var middleName: String, var surname: String, var photoUri: String, var number: String,
                   var email: String, var source: String) : Comparable<Contact> {
    companion object {
        var sorting: Int = 0
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> compareStrings(firstName, other.firstName)
            sorting and SORT_BY_MIDDLE_NAME != 0 -> compareStrings(middleName, other.middleName)
            sorting and SORT_BY_SURNAME != 0 -> compareStrings(surname, other.surname)
            else -> number.toLowerCase().compareTo(other.number.toLowerCase())
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
