package com.simplemobiletools.contacts.models

import android.graphics.Bitmap
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING

data class Contact(val id: Int, var prefix: String, var firstName: String, var middleName: String, var surname: String, var suffix: String, var photoUri: String,
                   var phoneNumbers: ArrayList<PhoneNumber>, var emails: ArrayList<Email>, var addresses: ArrayList<Address>, var events: ArrayList<Event>,
                   var source: String, var starred: Int, val contactId: Int, val thumbnailUri: String, var photo: Bitmap?, var notes: String,
                   var groups: ArrayList<Group>, var organization: Organization, var websites: ArrayList<String>) : Comparable<Contact> {
    companion object {
        var sorting = 0
        var startWithSurname = false
        val pattern = "\\D+".toRegex()
    }

    override fun compareTo(other: Contact): Int {
        val firstString: String
        val secondString: String

        when {
            sorting and SORT_BY_FIRST_NAME != 0 -> {
                firstString = firstName
                secondString = other.firstName
            }
            sorting and SORT_BY_MIDDLE_NAME != 0 -> {
                firstString = middleName
                secondString = other.middleName
            }
            else -> {
                firstString = surname
                secondString = other.surname
            }
        }

        var result = if (firstString.firstOrNull()?.isLetter() == true && secondString.firstOrNull()?.isLetter() == false) {
            -1
        } else if (firstString.firstOrNull()?.isLetter() == false && secondString.firstOrNull()?.isLetter() == true) {
            1
        } else {
            if (firstString.isEmpty() && secondString.isNotEmpty()) {
                1
            } else if (firstString.isNotEmpty() && secondString.isEmpty()) {
                -1
            } else {
                if (firstString.toLowerCase() == secondString.toLowerCase()) {
                    getFullName().compareTo(other.getFullName(), true)
                } else {
                    firstString.compareTo(secondString, true)
                }
            }
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

    fun getFullName(): String {
        var firstPart = if (startWithSurname) surname else firstName
        if (middleName.isNotEmpty()) {
            firstPart += " $middleName"
        }

        val lastPart = if (startWithSurname) firstName else surname
        val suffixComma = if (suffix.isEmpty()) "" else ", $suffix"
        val fullName = "$prefix $firstPart $lastPart$suffixComma".trim()
        return if (fullName.isEmpty()) {
            var fullOrganization = if (organization.jobPosition.isEmpty()) "" else "${organization.jobPosition}, "
            fullOrganization += organization.company
            fullOrganization.trim().trimEnd(',')
        } else {
            fullName
        }
    }

    fun getHashToCompare(): Int {
        val newPhoneNumbers = ArrayList<PhoneNumber>()
        phoneNumbers.mapTo(newPhoneNumbers) { PhoneNumber(it.value.replace(pattern, ""), 0) }

        val newEmails = ArrayList<Email>()
        emails.mapTo(newEmails) { Email(it.value, 0) }

        return copy(id = 0, prefix = "", firstName = getFullName().toLowerCase(), middleName = "", surname = "", suffix = "", photoUri = "",
                phoneNumbers = newPhoneNumbers, events = ArrayList(), addresses = ArrayList(), emails = newEmails, source = "", starred = 0,
                contactId = 0, thumbnailUri = "", notes = "", groups = ArrayList(), websites = ArrayList(), organization = Organization("", "")).hashCode()
    }
}
