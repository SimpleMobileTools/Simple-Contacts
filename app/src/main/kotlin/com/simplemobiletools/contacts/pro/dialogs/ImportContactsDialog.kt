package com.simplemobiletools.contacts.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getPublicContactSource
import com.simplemobiletools.contacts.pro.extensions.showContactSourcePicker
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.pro.helpers.VcfImporter
import com.simplemobiletools.contacts.pro.helpers.VcfImporter.ImportResult.IMPORT_FAIL
import kotlinx.android.synthetic.main.dialog_import_contacts.view.*

class ImportContactsDialog(val activity: SimpleActivity, val path: String, private val callback: (refreshView: Boolean) -> Unit) {
    private var targetContactSource = ""
    private var ignoreClicks = false

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_contacts, null) as ViewGroup).apply {
            targetContactSource = activity.config.lastUsedContactSource
            activity.getPublicContactSource(targetContactSource) {
                import_contacts_title.text = it
                if (it.isEmpty()) {
                    ContactsHelper(activity).getContactSources {
                        val localSource = it.firstOrNull { it.name == SMT_PRIVATE }
                        if (localSource != null) {
                            targetContactSource = localSource.name
                            activity.runOnUiThread {
                                import_contacts_title.text = localSource.publicName
                            }
                        }
                    }
                }
            }

            import_contacts_title.setOnClickListener {
                activity.showContactSourcePicker(targetContactSource) {
                    targetContactSource = if (it == activity.getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
                    activity.getPublicContactSource(it) {
                        import_contacts_title.text = it
                    }
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.import_contacts) {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            if (ignoreClicks) {
                                return@setOnClickListener
                            }

                            ignoreClicks = true
                            activity.toast(R.string.importing)
                            ensureBackgroundThread {
                                val result = VcfImporter(activity).importContacts(path, targetContactSource)
                                handleParseResult(result)
                                dismiss()
                            }
                        }
                    }
                }
    }

    private fun handleParseResult(result: VcfImporter.ImportResult) {
        activity.toast(when (result) {
            VcfImporter.ImportResult.IMPORT_OK -> R.string.importing_successful
            VcfImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
            else -> R.string.importing_failed
        })
        callback(result != IMPORT_FAIL)
    }
}
