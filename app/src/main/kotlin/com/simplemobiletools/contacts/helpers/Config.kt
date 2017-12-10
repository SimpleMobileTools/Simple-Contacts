package com.simplemobiletools.contacts.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_NAME

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var sorting: Int
        get() = prefs.getInt(SORTING, SORT_BY_NAME)
        set(sorting) = prefs.edit().putInt(SORTING, sorting).apply()
}
