package com.simplemobiletools.contacts.models

import com.simplemobiletools.commons.helpers.SORT_BY_NUMBER
import com.simplemobiletools.commons.helpers.SORT_DESCENDING

data class Contact(val id: Int, var name: String, var number: String, var photoUri: String) : Comparable<Contact> {
    companion object {
        var sorting: Int = 0
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            (sorting and SORT_BY_NUMBER != 0) -> number.toLowerCase().compareTo(other.number.toLowerCase())
            else -> if (name.first().isLetter() && !other.name.first().isLetter()) {
                -1
            } else if (!name.first().isLetter() && other.name.first().isLetter()) {
                1
            } else {
                name.toLowerCase().compareTo(other.name.toLowerCase())
            }
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    fun getBubbleText() = when {
        sorting and SORT_BY_NUMBER != 0 -> number
        else -> name
    }
}
