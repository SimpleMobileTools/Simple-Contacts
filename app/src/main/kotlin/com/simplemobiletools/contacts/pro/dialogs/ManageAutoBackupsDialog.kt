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
import com.simplemobiletools.contacts.pro.databinding.DialogManageAutomaticBackupsBinding
import com.simplemobiletools.contacts.pro.extensions.config
import java.io.File

class ManageAutoBackupsDialog(private val activity: SimpleActivity, onSuccess: () -> Unit) {
    private val binding = DialogManageAutomaticBackupsBinding.inflate(activity.layoutInflater)
    private val config = activity.config
    private var backupFolder = config.autoBackupFolder
    private var contactSources = mutableListOf<ContactSource>()
    private var selectedContactSources = config.autoBackupContactSources
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        binding.apply {
            backupContactsFolder.setText(activity.humanizePath(backupFolder))
            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.contacts)}_%Y%M%D_%h%m%s"
            }

            backupContactsFilename.setText(filename)
            backupContactsFilenameHint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }

            backupContactsFilenameHint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backupContactsFolder.setOnClickListener {
                selectBackupFolder()
            }

            ContactsHelper(activity).getContactSources { sources ->
                contactSources = sources
                isContactSourcesReady = true
                processDataIfReady(this)
            }

            ContactsHelper(activity).getContacts(getAll = true) { receivedContacts ->
                contacts = receivedContacts
                isContactsReady = true
                processDataIfReady(this)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.simplemobiletools.commons.R.string.manage_automatic_backups) { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (binding.backupContactSourcesList.adapter == null) {
                            return@setOnClickListener
                        }
                        val filename = binding.backupContactsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(com.simplemobiletools.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(backupFolder, "$filename.vcf")
                                if (file.exists() && !file.canWrite()) {
                                    activity.toast(com.simplemobiletools.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val selectedSources = (binding.backupContactSourcesList.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                if (selectedSources.isEmpty()) {
                                    activity.toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                config.autoBackupContactSources = selectedSources.map { it.name }.toSet()

                                ensureBackgroundThread {
                                    config.apply {
                                        autoBackupFolder = backupFolder
                                        autoBackupFilename = filename
                                    }

                                    activity.runOnUiThread {
                                        onSuccess()
                                    }

                                    dialog.dismiss()
                                }
                            }

                            else -> activity.toast(com.simplemobiletools.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }

    private fun processDataIfReady(binding: DialogManageAutomaticBackupsBinding) {
        if (!isContactSourcesReady || !isContactsReady) {
            return
        }

        if (selectedContactSources.isEmpty()) {
            selectedContactSources = contactSources.map { it.name }.toSet()
        }

        val contactSourcesWithCount = mutableListOf<ContactSource>()
        for (source in contactSources) {
            val count = contacts.count { it.source == source.name }
            contactSourcesWithCount.add(source.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            binding.backupContactSourcesList.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedContactSources.toList())
        }
    }

    private fun selectBackupFolder() {
        activity.hideKeyboard(binding.backupContactsFilename)
        FilePickerDialog(activity, backupFolder, false, showFAB = true) { path ->
            activity.handleSAFDialog(path) { grantedSAF ->
                if (!grantedSAF) {
                    return@handleSAFDialog
                }

                activity.handleSAFDialogSdk30(path) { grantedSAF30 ->
                    if (!grantedSAF30) {
                        return@handleSAFDialogSdk30
                    }

                    backupFolder = path
                    binding.backupContactsFolder.setText(activity.humanizePath(path))
                }
            }
        }
    }
}

