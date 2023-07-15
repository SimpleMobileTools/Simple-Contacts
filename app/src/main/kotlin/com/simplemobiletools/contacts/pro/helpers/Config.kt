package com.simplemobiletools.contacts.pro.helpers

import android.content.Context
import android.os.Environment
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SHOW_TABS

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var autoBackup: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP, false)
        set(enableAutomaticBackups) = prefs.edit().putBoolean(AUTO_BACKUP, enableAutomaticBackups).apply()

    var autoBackupFolder: String
        get() = prefs.getString(AUTO_BACKUP_FOLDER, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)!!
        set(autoBackupPath) = prefs.edit().putString(AUTO_BACKUP_FOLDER, autoBackupPath).apply()

    var autoBackupFilename: String
        get() = prefs.getString(AUTO_BACKUP_FILENAME, "")!!
        set(autoBackupFilename) = prefs.edit().putString(AUTO_BACKUP_FILENAME, autoBackupFilename).apply()

    var autoBackupContactSources: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CONTACT_SOURCES, setOf())!!
        set(autoBackupContactTypes) = prefs.edit().remove(AUTO_BACKUP_CONTACT_SOURCES).putStringSet(AUTO_BACKUP_CONTACT_SOURCES, autoBackupContactTypes).apply()

    var lastAutoBackupTime: Long
        get() = prefs.getLong(LAST_AUTO_BACKUP_TIME, 0L)
        set(lastAutoBackupTime) = prefs.edit().putLong(LAST_AUTO_BACKUP_TIME, lastAutoBackupTime).apply()
}
