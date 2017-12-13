package com.simplemobiletools.contacts.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_FIRST_NAME

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var sorting: Int
        get() = prefs.getInt(SORTING, SORT_BY_FIRST_NAME)
        set(sorting) = prefs.edit().putInt(SORTING, sorting).apply()

    var callContact: Boolean
        get() = prefs.getBoolean(CALL_CONTACT_ON_CLICK, false)
        set(callContact) = prefs.edit().putBoolean(CALL_CONTACT_ON_CLICK, callContact).apply()

    var displayContactSources: Set<String>
        get() = prefs.getStringSet(DISPLAY_CONTACT_SOURCES, hashSetOf("-1"))
        set(displayContactSources) = prefs.edit().remove(DISPLAY_CONTACT_SOURCES).putStringSet(DISPLAY_CONTACT_SOURCES, displayContactSources).apply()

    fun showAllContacts() = displayContactSources.size == 1 && displayContactSources.first() == "-1"

    var startNameWithSurname: Boolean
        get() = prefs.getBoolean(START_NAME_WITH_SURNAME, false)
        set(startNameWithSurname) = prefs.edit().putBoolean(START_NAME_WITH_SURNAME, startNameWithSurname).apply()
}
