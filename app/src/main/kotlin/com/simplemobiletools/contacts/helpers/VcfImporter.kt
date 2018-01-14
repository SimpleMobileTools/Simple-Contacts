package com.simplemobiletools.contacts.helpers

import android.widget.Toast
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.helpers.VcfImporter.ImportResult.*
import java.io.File

class VcfImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private var contactsImported = 0
    private var contactsFailed = 0

    fun importContacts(path: String, targetContactSource: String): ImportResult {
        try {
            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            inputStream.bufferedReader().use {
                while (true) {
                    val line = it.readLine() ?: break
                    if (line.trim().isEmpty())
                        continue

                    if (line == BEGIN_VCARD) {
                        resetValues()
                    } else if (line == END_VCARD) {
                        contactsImported++
                    }
                }
            }
        } catch (e: Exception) {
            activity.showErrorToast(e, Toast.LENGTH_LONG)
            contactsFailed++
        }

        return when {
            contactsImported == 0 -> IMPORT_FAIL
            contactsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun resetValues() {

    }
}
