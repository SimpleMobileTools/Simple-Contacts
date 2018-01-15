package com.simplemobiletools.contacts.helpers

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.contacts.helpers.VcfExporter.ExportResult.*
import com.simplemobiletools.contacts.models.Contact
import java.io.File

class VcfExporter() {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var contactsExported = 0
    private var contactsFailed = 0

    fun exportContacts(activity: BaseSimpleActivity, file: File, contacts: ArrayList<Contact>, callback: (result: ExportResult) -> Unit) {
        activity.getFileOutputStream(file) {
            if (it == null) {
                callback(EXPORT_FAIL)
                return@getFileOutputStream
            }

            it.bufferedWriter().use { out ->
                for (contact in contacts) {

                }
            }
        }

        callback(when {
            contactsExported == 0 -> EXPORT_FAIL
            contactsFailed > 0 -> EXPORT_PARTIAL
            else -> EXPORT_OK
        })
    }
}
