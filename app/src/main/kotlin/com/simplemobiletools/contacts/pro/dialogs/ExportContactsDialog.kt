package com.simplemobiletools.contacts.pro.dialogs

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.contacts.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.FilterContactSourcesAdapter
import kotlinx.android.synthetic.main.dialog_export_contacts.view.*
import java.io.File

class ExportContactsDialog(
    val activity: SimpleActivity,
    val path: String,
    val hidePath: Boolean,
    private val callback: (file: File, ignoredContactSources: HashSet<String>) -> Unit
) {
    private var ignoreClicks = false
    private var realPath = if (path.isEmpty()) activity.internalStoragePath else path
    private var contactSources = ArrayList<ContactSource>()
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_contacts, null) as ViewGroup).apply {
            export_contacts_folder.setText(activity.humanizePath(realPath))
            export_contacts_filename.setText("contacts_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                export_contacts_folder_hint.beGone()
            } else {
                export_contacts_folder.setOnClickListener {
                    activity.hideKeyboard(export_contacts_filename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        export_contacts_folder.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }

            ContactsHelper(activity).getContactSources { contactSources ->
                contactSources.mapTo(this@ExportContactsDialog.contactSources) { it.copy() }
                isContactSourcesReady = true
                processDataIfReady(this)
            }

            ContactsHelper(activity).getContacts(getAll = true) { contacts ->
                contacts.mapTo(this@ExportContactsDialog.contacts) { it.copy() }
                isContactsReady = true
                processDataIfReady(this)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.export_contacts) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (view.export_contacts_list.adapter == null || ignoreClicks) {
                            return@setOnClickListener
                        }

                        val filename = view.export_contacts_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.vcf")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ignoreClicks = true
                                ensureBackgroundThread {
                                    activity.baseConfig.lastExportPath = file.absolutePath.getParentPath()
                                    val selectedSources = (view.export_contacts_list.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                    val ignoredSources = contactSources
                                        .filter { !selectedSources.contains(it) }
                                        .map { it.getFullIdentifier() }
                                        .toHashSet()
                                    callback(file, ignoredSources)
                                    alertDialog.dismiss()
                                }
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }

    private fun processDataIfReady(view: View) {
        if (!isContactSourcesReady || !isContactsReady) {
            return
        }

        val contactSourcesWithCount = ArrayList<ContactSource>()
        for (source in contactSources) {
            val count = contacts.filter { it.source == source.name }.count()
            contactSourcesWithCount.add(source.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            view.export_contacts_list.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, activity.getVisibleContactSources())
        }
    }
}
