package com.simplemobiletools.contacts.pro.models

import android.graphics.Bitmap
import android.telephony.PhoneNumberUtils
import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_MIDDLE_NAME
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.contacts.pro.extensions.normalizeNumber
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE

data class Contact(var id: Int, var prefix: String, var firstName: String, var middleName: String, var surname: String, var suffix: String, var nickname: String,
                   var photoUri: String, var phoneNumbers: ArrayList<PhoneNumber>, var emails: ArrayList<Email>, var addresses: ArrayList<Address>,
                   var events: ArrayList<Event>, var source: String, var starred: Int, var contactId: Int, var thumbnailUri: String, var photo: Bitmap?, var notes: String,
                   var groups: ArrayList<Group>, var organization: Organization, var websites: ArrayList<String>, var IMs: ArrayList<IM>) :
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
            val fullCompany = getFullCompany()
            if (fullCompany.isNotEmpty()) {
                firstString = fullCompany.normalizeString()
            } else if (emails.isNotEmpty()) {
                firstString = emails.first().value
            }
        }

        if (secondString.isEmpty() && other.firstName.isEmpty() && other.middleName.isEmpty() && other.surname.isEmpty()) {
            val otherFullCompany = other.getFullCompany()
            if (otherFullCompany.isNotEmpty()) {
                secondString = otherFullCompany.normalizeString()
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
                getFullCompany()
            } else {
                emails.firstOrNull()?.value?.trim() ?: ""
            }
        } else {
            fullName
        }
    }

    fun getStringToCompare(): String {
        return copy(id = 0, prefix = "", firstName = getNameToDisplay().toLowerCase(), middleName = "", surname = "", suffix = "", nickname = "", photoUri = "",
                phoneNumbers = ArrayList(), emails = ArrayList(), events = ArrayList(), source = "", addresses = ArrayList(), starred = 0, contactId = 0,
                thumbnailUri = "", notes = "", groups = ArrayList(), websites = ArrayList(), organization = Organization("", ""), IMs = ArrayList()).toString()
    }

    fun getHashToCompare() = getStringToCompare().hashCode()

    fun getFullCompany(): String {
        var fullOrganization = if (organization.company.isEmpty()) "" else "${organization.company}, "
        fullOrganization += organization.jobPosition
        return fullOrganization.trim().trimEnd(',')
    }

    fun isABusinessContact() = prefix.isEmpty() && firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty() && suffix.isEmpty() && organization.isNotEmpty()

    fun doesContainPhoneNumber(text: String, convertLetters: Boolean): Boolean {
        return if (text.isNotEmpty()) {
            val normalizedText = if (convertLetters) text.normalizeNumber() else text
            phoneNumbers.any {
                PhoneNumberUtils.compare(it.normalizedNumber, normalizedText) ||
                        it.value.contains(text) ||
                        it.normalizedNumber?.contains(normalizedText) == true ||
                        it.value.normalizeNumber().contains(normalizedText)
            }
        } else {
            false
        }
    }

    fun isPrivate() = source == SMT_PRIVATE
}
