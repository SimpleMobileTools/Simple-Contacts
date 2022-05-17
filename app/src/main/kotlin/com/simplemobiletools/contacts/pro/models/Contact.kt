package com.simplemobiletools.contacts.pro.models

import android.graphics.Bitmap
import android.telephony.PhoneNumberUtils
import com.simplemobiletools.commons.extensions.normalizePhoneNumber
import com.simplemobiletools.commons.extensions.normalizeString
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.PhoneNumber
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE

data class Contact(
    var id: Int, var prefix: String, var firstName: String, var middleName: String, var surname: String, var suffix: String, var nickname: String,
    var photoUri: String, var phoneNumbers: ArrayList<PhoneNumber>, var emails: ArrayList<Email>, var addresses: ArrayList<Address>,
    var events: ArrayList<Event>, var source: String, var starred: Int, var contactId: Int, var thumbnailUri: String, var photo: Bitmap?, var notes: String,
    var groups: ArrayList<Group>, var organization: Organization, var websites: ArrayList<String>, var IMs: ArrayList<IM>, var mimetype: String,
    var ringtone: String?
) :
    Comparable<Contact> {
    companion object {
        var sorting = 0
        var startWithSurname = false
    }

    override fun compareTo(other: Contact): Int {
        var result = when {
            sorting and SORT_BY_FIRST_NAME != 0 -> {
                val firstString = firstName.normalizeString()
                val secondString = other.firstName.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_MIDDLE_NAME != 0 -> {
                val firstString = middleName.normalizeString()
                val secondString = other.middleName.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_SURNAME != 0 -> {
                val firstString = surname.normalizeString()
                val secondString = other.surname.normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            sorting and SORT_BY_FULL_NAME != 0 -> {
                val firstString = getNameToDisplay().normalizeString()
                val secondString = other.getNameToDisplay().normalizeString()
                compareUsingStrings(firstString, secondString, other)
            }
            else -> compareUsingIds(other)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }

        return result
    }

    private fun compareUsingStrings(firstString: String, secondString: String, other: Contact): Int {
        var firstValue = firstString
        var secondValue = secondString

        if (firstValue.isEmpty() && firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty()) {
            val fullCompany = getFullCompany()
            if (fullCompany.isNotEmpty()) {
                firstValue = fullCompany.normalizeString()
            } else if (emails.isNotEmpty()) {
                firstValue = emails.first().value
            }
        }

        if (secondValue.isEmpty() && other.firstName.isEmpty() && other.middleName.isEmpty() && other.surname.isEmpty()) {
            val otherFullCompany = other.getFullCompany()
            if (otherFullCompany.isNotEmpty()) {
                secondValue = otherFullCompany.normalizeString()
            } else if (other.emails.isNotEmpty()) {
                secondValue = other.emails.first().value
            }
        }

        return if (firstValue.firstOrNull()?.isLetter() == true && secondValue.firstOrNull()?.isLetter() == false) {
            -1
        } else if (firstValue.firstOrNull()?.isLetter() == false && secondValue.firstOrNull()?.isLetter() == true) {
            1
        } else {
            if (firstValue.isEmpty() && secondValue.isNotEmpty()) {
                1
            } else if (firstValue.isNotEmpty() && secondValue.isEmpty()) {
                -1
            } else {
                if (firstValue.equals(secondValue, ignoreCase = true)) {
                    getNameToDisplay().compareTo(other.getNameToDisplay(), true)
                } else {
                    firstValue.compareTo(secondValue, true)
                }
            }
        }
    }

    private fun compareUsingIds(other: Contact): Int {
        val firstId = id
        val secondId = other.id
        return firstId.compareTo(secondId)
    }

    fun getBubbleText() = when {
        sorting and SORT_BY_FIRST_NAME != 0 -> firstName
        sorting and SORT_BY_MIDDLE_NAME != 0 -> middleName
        else -> surname
    }

    fun getNameToDisplay(): String {
        val firstMiddle = "$firstName $middleName".trim()
        val firstPart = if (startWithSurname) if (surname.isNotEmpty() && firstMiddle.isNotEmpty()) "$surname," else surname else firstMiddle
        val lastPart = if (startWithSurname) firstMiddle else surname
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

    // photos stored locally always have different hashcodes. Avoid constantly refreshing the contact lists as the app thinks something changed.
    fun getHashWithoutPrivatePhoto(): Int {
        val photoToUse = if (isPrivate()) null else photo
        return copy(photo = photoToUse).hashCode()
    }

    fun getStringToCompare(): String {
        val photoToUse = if (isPrivate()) null else photo
        return copy(
            id = 0,
            prefix = "",
            firstName = getNameToDisplay().toLowerCase(),
            middleName = "",
            surname = "",
            suffix = "",
            nickname = "",
            photoUri = "",
            phoneNumbers = ArrayList(),
            emails = ArrayList(),
            events = ArrayList(),
            source = "",
            addresses = ArrayList(),
            starred = 0,
            contactId = 0,
            thumbnailUri = "",
            photo = photoToUse,
            notes = "",
            groups = ArrayList(),
            websites = ArrayList(),
            organization = Organization("", ""),
            IMs = ArrayList(),
            ringtone = ""
        ).toString()
    }

    fun getHashToCompare() = getStringToCompare().hashCode()

    fun getFullCompany(): String {
        var fullOrganization = if (organization.company.isEmpty()) "" else "${organization.company}, "
        fullOrganization += organization.jobPosition
        return fullOrganization.trim().trimEnd(',')
    }

    fun isABusinessContact() =
        prefix.isEmpty() && firstName.isEmpty() && middleName.isEmpty() && surname.isEmpty() && suffix.isEmpty() && organization.isNotEmpty()

    fun doesContainPhoneNumber(text: String, convertLetters: Boolean): Boolean {
        return if (text.isNotEmpty()) {
            val normalizedText = if (convertLetters) text.normalizePhoneNumber() else text
            phoneNumbers.any {
                PhoneNumberUtils.compare(it.normalizedNumber, normalizedText) ||
                    it.value.contains(text) ||
                    it.normalizedNumber.contains(normalizedText) ||
                    it.value.normalizePhoneNumber().contains(normalizedText)
            }
        } else {
            false
        }
    }

    fun isPrivate() = source == SMT_PRIVATE

    fun getSignatureKey() = if (photoUri.isNotEmpty()) photoUri else hashCode()
}
