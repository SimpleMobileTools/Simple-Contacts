package com.simplemobiletools.contacts.dialogs

import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.R
import com.simplemobiletools.contacts.activities.SimpleActivity
import com.simplemobiletools.contacts.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.extensions.config
import com.simplemobiletools.contacts.helpers.ContactsHelper
import com.simplemobiletools.contacts.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.models.ContactSource
import kotlinx.android.synthetic.main.dialog_export_contacts.view.*
import java.io.File
import java.util.*

class ExportContactsDialog(val activity: SimpleActivity, val path: String, private val callback: (file: File, contactSources: HashSet<String>) -> Unit) {
    private var contactSources = ArrayList<ContactSource>()

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_contacts, null) as ViewGroup).apply {
            export_contacts_folder.text = activity.humanizePath(path)
            export_contacts_filename.setText("contacts_${activity.getCurrentFormattedDateTime()}")

            ContactsHelper(activity).getContactSources {
                it.mapTo(contactSources, { it.copy() })
                activity.runOnUiThread {
                    export_contacts_list.adapter = FilterContactSourcesAdapter(activity, it, activity.config.displayContactSources)
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.export_contacts) {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (view.export_contacts_list.adapter == null) {
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

                            val selectedIndexes = (view.export_contacts_list.adapter as FilterContactSourcesAdapter).getSelectedItemsSet()
                            val selectedContactSources = HashSet<String>()
                            selectedIndexes.forEach {
                                selectedContactSources.add(if (contactSources[it].type == SMT_PRIVATE) SMT_PRIVATE else contactSources[it].name)
                            }
                            callback(file, selectedContactSources)
                            dismiss()
                        }
                        else -> activity.toast(R.string.invalid_name)
                    }
                }
            }
        }
    }
}
