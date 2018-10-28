package com.simplemobiletools.contacts.models

import android.graphics.Bitmap
import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.extensions.applyRegexFiltering

data class Contact(val id: Int, var prefix: String, var firstName: String, var middleName: String, var surname: String, var suffix: String, var nickname: String,
                   var photoUri: String, var phoneNumbers: ArrayList<PhoneNumber>, var emails: ArrayList<Email>, var addresses: ArrayList<Address>,
                   var events: ArrayList<Event>, var source: String, var starred: Int, val contactId: Int, val thumbnailUri: String, var photo: Bitmap?, var notes: String,
                   var groups: ArrayList<Group>, var organization: Organization, var websites: ArrayList<String>, var cleanPhoneNumbers: ArrayList<PhoneNumber>,
                   var IMs: ArrayList<IM>) :
        Comparable<Contact> {
    companion object {
        var sorting = 0
        var startWithSurname = false
    }

    override fun compareTo(other: Contact): Int {
        var firstString: String
        var secondString: String

        when {
            sorting and SORT_BY_FIRST_NAME != 0 -> {
                firstString = firstName.normalizeString()
                secondString = other.firstName.normalizeString()
            }
            sorting and SORT_BY_MIDDLE_NAME != 0 -> {
                firstString = middleName.normalizeString()
                secondString = other.middleName.normalizeString()
            }
            else -> {
                firstString = surname.normalizeString()
                secondString = other.surname.normalizeString()
            }
        }

        if (firstString.isEmpty() && firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty()) {
            if (organization.company.isNotEmpty()) {
                firstString = organization.company.normalizeString()
            } else if (emails.isNotEmpty()) {
                firstString = emails.first().value
            }
        }

        if (secondString.isEmpty() && other.firstName.isEmpty() && other.middleName.isEmpty() && other.surname.isEmpty()) {
            if (other.organization.company.isNotEmpty()) {
                secondString = other.organization.company.normalizeString()
            } else if (other.emails.isNotEmpty()) {
                secondString = other.emails.first().value
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
                    getNameToDisplay().compareTo(other.getNameToDisplay(), true)
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

    fun getNameToDisplay(): String {
        var firstPart = if (startWithSurname) surname else firstName
        if (middleName.isNotEmpty()) {
            firstPart += " $middleName"
        }

        val lastPart = if (startWithSurname) firstName else surname
        val suffixComma = if (suffix.isEmpty()) "" else ", $suffix"
        val fullName = "$prefix $firstPart $lastPart$suffixComma".trim()
        return if (fullName.isEmpty()) {
            if (organization.isNotEmpty()) {
                var fullOrganization = if (organization.jobPosition.isEmpty()) "" else "${organization.jobPosition}, "
                fullOrganization += organization.company
                fullOrganization.trim().trimEnd(',')
            } else {
                emails.firstOrNull()?.value?.trim() ?: ""
            }
        } else {
            fullName
        }
    }

    fun getStringToCompare(): String {
        val newEmails = ArrayList<Email>()
        emails.mapTo(newEmails) { Email(it.value, 0, "") }

        return copy(id = 0, prefix = "", firstName = getNameToDisplay().toLowerCase(), middleName = "", surname = "", suffix = "", nickname = "", photoUri = "",
                phoneNumbers = ArrayList(), events = ArrayList(), addresses = ArrayList(), emails = newEmails, source = "", starred = 0,
                contactId = 0, thumbnailUri = "", notes = "", groups = ArrayList(), websites = ArrayList(), organization = Organization("", ""),
                IMs = ArrayList()).toString()
    }

    fun getHashToCompare() = getStringToCompare().hashCode()

    // do a more advanced phone number check here, compare numbers and and search query with dashes, spaces and everything but numbers removed
    fun doesContainPhoneNumber(text: String): Boolean {
        if (text.isNotEmpty()) {
            if (phoneNumbers.any { it.value.contains(text) } || cleanPhoneNumbers.any { it.value.contains(text) }) {
                return true
            }
        }

        val filteredNumber = text.applyRegexFiltering()
        if (filteredNumber.isNotEmpty()) {
            if (phoneNumbers.any { it.value.contains(filteredNumber) } || cleanPhoneNumbers.any { it.value.contains(filteredNumber) }) {
                return true
            }
        }

        return false
    }
}
