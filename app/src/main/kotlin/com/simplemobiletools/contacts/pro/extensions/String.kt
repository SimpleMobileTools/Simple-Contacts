package com.simplemobiletools.contacts.pro.extensions

import android.telephony.PhoneNumberUtils
import android.widget.TextView
import com.simplemobiletools.commons.helpers.getDateFormats
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun String.getDateTimeFromDateString(viewToUpdate: TextView? = null): DateTime {
    val dateFormats = getDateFormats()
    var date = DateTime()
    for (format in dateFormats) {
        try {
            date = DateTime.parse(this, DateTimeFormat.forPattern(format))

            val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            var localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()

            val hasYear = format.contains("y")
            if (!hasYear) {
                localPattern = localPattern.replace("y", "").trim()
                date = date.withYear(DateTime().year)
            }

            val formattedString = date.toString(localPattern)
            viewToUpdate?.text = formattedString
            break
        } catch (ignored: Exception) {
        }
    }
    return date
}

fun String.normalizeNumber() = PhoneNumberUtils.normalizeNumber(this)
