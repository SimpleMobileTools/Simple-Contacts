package com.simplemobiletools.contacts.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.pro.extensions.getVisibleContactSources
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.ContactSource
import kotlinx.android.synthetic.main.dialog_export_contacts.view.*
import java.io.File
import java.util.*

class ExportContactsDialog(val activity: SimpleActivity, val path: String, private val callback: (file: File, ignoredContactSources: HashSet<String>) -> Unit) {
    private var contactSources = ArrayList<ContactSource>()
    private var ignoreClicks = false

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_contacts, null) as ViewGroup).apply {
            export_contacts_folder.text = activity.humanizePath(path)
            export_contacts_filename.setText("contacts_${activity.getCurrentFormattedDateTime()}")

            ContactsHelper(activity).getContactSources {
                it.mapTo(contactSources) { it.copy() }
                activity.runOnUiThread {
                    export_contacts_list.adapter = FilterContactSourcesAdapter(activity, it, activity.getVisibleContactSources())
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.export_contacts) {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            if (view.export_contacts_list.adapter == null || ignoreClicks) {
                                return@setOnClickListener
                            }

                            val filename = view.export_contacts_filename.value
                            when {
                                filename.isEmpty() -> activity.toast(R.string.empty_name)
                                filename.isAValidFilename() -> {
                                    val file = File(path, "$filename.vcf")
                                    if (file.exists()) {
                                        activity.toast(R.string.name_taken)
                                        return@setOnClickListener
                                    }

                                    ignoreClicks = true
                                    ensureBackgroundThread {
                                        val selectedSources = (view.export_contacts_list.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                        val ignoredSources = contactSources.filter { !selectedSources.contains(it) }.map { it.getFullIdentifier() }.toHashSet()
                                        callback(file, ignoredSources)
                                        dismiss()
                                    }
                                }
                                else -> activity.toast(R.string.invalid_name)
                            }
                        }
                    }
                }
    }
}
