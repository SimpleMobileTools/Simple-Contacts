package com.simplemobiletools.contacts.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.models.contacts.ContactSource
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.adapters.FilterContactSourcesAdapter
import com.simplemobiletools.contacts.pro.databinding.DialogExportContactsBinding
import com.simplemobiletools.contacts.pro.extensions.config
import java.io.File

class ExportContactsDialog(
    val activity: SimpleActivity, val path: String, val hidePath: Boolean,
    private val callback: (file: File, ignoredContactSources: HashSet<String>) -> Unit
) {
    private var ignoreClicks = false
    private var realPath = if (path.isEmpty()) activity.internalStoragePath else path
    private var contactSources = ArrayList<ContactSource>()
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        val binding = DialogExportContactsBinding.inflate(activity.layoutInflater).apply {
            exportContactsFolder.setText(activity.humanizePath(realPath))
            exportContactsFilename.setText("contacts_${activity.getCurrentFormattedDateTime()}")

            if (hidePath) {
                exportContactsFolderHint.beGone()
            } else {
                exportContactsFolder.setOnClickListener {
                    activity.hideKeyboard(exportContactsFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportContactsFolder.setText(activity.humanizePath(it))
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
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_contacts) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (binding.exportContactsList.adapter == null || ignoreClicks) {
                            return@setOnClickListener
                        }

                        val filename = binding.exportContactsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(com.simplemobiletools.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.vcf")
                                if (!hidePath && file.exists()) {
                                    activity.toast(com.simplemobiletools.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ignoreClicks = true
                                ensureBackgroundThread {
                                    activity.config.lastExportPath = file.absolutePath.getParentPath()
                                    val selectedSources = (binding.exportContactsList.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                    val ignoredSources = contactSources
                                        .filter { !selectedSources.contains(it) }
                                        .map { it.getFullIdentifier() }
                                        .toHashSet()
                                    callback(file, ignoredSources)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(com.simplemobiletools.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }

    private fun processDataIfReady(binding: DialogExportContactsBinding) {
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
            binding.exportContactsList.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, activity.getVisibleContactSources())
        }
    }
}
