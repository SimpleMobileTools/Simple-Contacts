package com.simplemobiletools.contacts.pro.dialogs

import android.view.View
import android.view.ViewGroup
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
import com.simplemobiletools.contacts.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_contact_sources_list
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_contacts_filename
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_contacts_filename_hint
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_contacts_folder
import java.io.File

class ManageAutoBackupsDialog(private val activity: SimpleActivity, onSuccess: () -> Unit) {
    private val view = (activity.layoutInflater.inflate(R.layout.dialog_manage_automatic_backups, null) as ViewGroup)
    private val config = activity.config
    private var backupFolder = config.autoBackupFolder
    private var contactSources = mutableListOf<ContactSource>()
    private var selectedContactSources = config.autoBackupContactSources
    private var contacts = ArrayList<Contact>()
    private var isContactSourcesReady = false
    private var isContactsReady = false

    init {
        view.apply {
            backup_contacts_folder.setText(activity.humanizePath(backupFolder))
            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.contacts)}_%Y%M%D_%h%m%s"
            }

            backup_contacts_filename.setText(filename)
            backup_contacts_filename_hint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }

            backup_contacts_filename_hint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backup_contacts_folder.setOnClickListener {
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
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.manage_automatic_backups) { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (view.backup_contact_sources_list.adapter == null) {
                            return@setOnClickListener
                        }
                        val filename = view.backup_contacts_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(backupFolder, "$filename.vcf")
                                if (file.exists() && !file.canWrite()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val selectedSources = (view.backup_contact_sources_list.adapter as FilterContactSourcesAdapter).getSelectedContactSources()
                                if (selectedSources.isEmpty()) {
                                    activity.toast(R.string.no_entries_for_exporting)
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

        val contactSourcesWithCount = mutableListOf<ContactSource>()
        for (source in contactSources) {
            val count = contacts.filter { it.source == source.name }.count()
            contactSourcesWithCount.add(source.copy(count = count))
        }

        contactSources.clear()
        contactSources.addAll(contactSourcesWithCount)

        activity.runOnUiThread {
            view.backup_contact_sources_list.adapter = FilterContactSourcesAdapter(activity, contactSourcesWithCount, selectedContactSources.toList())
        }
    }

    private fun selectBackupFolder() {
        activity.hideKeyboard(view.backup_contacts_filename)
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
                    view.backup_contacts_folder.setText(activity.humanizePath(path))
                }
            }
        }
    }
}

