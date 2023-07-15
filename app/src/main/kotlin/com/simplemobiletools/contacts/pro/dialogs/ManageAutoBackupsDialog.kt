package com.simplemobiletools.contacts.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.isAValidFilename
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.contacts.ContactSource
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.SimpleActivity
import com.simplemobiletools.contacts.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_events_filename
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_events_filename_hint
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.backup_events_folder
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.manage_event_types_holder
import java.io.File

class ManageAutoBackupsDialog(private val activity: SimpleActivity, onSuccess: () -> Unit) {
    private val view = (activity.layoutInflater.inflate(R.layout.dialog_manage_automatic_backups, null) as ViewGroup)
    private val config = activity.config
    private var backupFolder = config.autoBackupFolder
    private var selectedContactTypes = HashSet<ContactSource>()

    private fun setContactTypes() {
        ContactsHelper(activity).getContactSources { contactSources ->
            val availableContactSources = contactSources.toSet()
            if (config.autoBackupContactSources.isEmpty()) {
                selectedContactTypes = contactSources.toHashSet()
            } else {
                selectedContactTypes = availableContactSources.filter { it.name in config.autoBackupContactSources }.toHashSet()
            }
        }
    }

    init {
        setContactTypes()
        view.apply {
            backup_events_folder.setText(activity.humanizePath(backupFolder))
            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.contacts)}_%Y%M%D_%h%m%s"
            }

            backup_events_filename.setText(filename)
            backup_events_filename_hint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }

            backup_events_filename_hint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backup_events_folder.setOnClickListener {
                selectBackupFolder()
            }

            manage_event_types_holder.setOnClickListener {
                activity.runOnUiThread {
                    SelectContactTypesDialog(activity, selectedContactTypes.map { it.name }) {
                        selectedContactTypes = it
                        config.autoBackupContactSources = it.map { it.name }.toSet()
                    }
                }
            }
        }
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.manage_automatic_backups) { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.backup_events_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(backupFolder, "$filename.ics")
                                if (file.exists() && !file.canWrite()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                if (selectedContactTypes.isEmpty()) {
                                    activity.toast(R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.apply {
                                        autoBackupFolder = backupFolder
                                        autoBackupFilename = filename
                                        if (autoBackupContactSources != selectedContactTypes) {
                                            autoBackupContactSources = selectedContactTypes.map { it.type }.toSet()
                                        }
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

    private fun selectBackupFolder() {
        activity.hideKeyboard(view.backup_events_filename)
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
                    view.backup_events_folder.setText(activity.humanizePath(path))
                }
            }
        }
    }
}

