package com.simplemobiletools.contacts.pro.dialogs

import android.annotation.SuppressLint
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.telecomManager
import com.simplemobiletools.contacts.pro.R
import kotlinx.android.synthetic.main.dialog_select_sim.view.*

@SuppressLint("MissingPermission")
class SelectSIMDialog(val activity: BaseSimpleActivity, val callback: (handle: PhoneAccountHandle) -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_sim, null)
        val radioGroup = view.select_sim_radio_group

        activity.telecomManager.callCapablePhoneAccounts.forEachIndexed { index, account ->
            val phoneAccount = activity.telecomManager.getPhoneAccount(account)
            var label = phoneAccount.label.toString()
            var address = phoneAccount.address.toString()
            if (address.startsWith("tel:") && address.substringAfter("tel:").isNotEmpty()) {
                address = Uri.decode(address.substringAfter("tel:"))
                label += " ($address)"
            }

            val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
                text = label
                id = index
                setOnClickListener { selectedSIM(phoneAccount.accountHandle) }
            }
            radioGroup!!.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        dialog = AlertDialog.Builder(activity)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun selectedSIM(handle: PhoneAccountHandle) {
        callback(handle)
        dialog?.dismiss()
    }
}
