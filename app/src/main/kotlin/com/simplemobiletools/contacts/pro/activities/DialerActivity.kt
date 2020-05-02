package com.simplemobiletools.contacts.pro.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.view.Menu
import com.simplemobiletools.commons.extensions.isDefaultDialer
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.telecomManager
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_READ_PHONE_STATE
import com.simplemobiletools.commons.helpers.REQUEST_CODE_SET_DEFAULT_DIALER
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.dialogs.SelectSIMDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getAvailableSIMCardLabels

class DialerActivity : SimpleActivity() {
    private var callNumber: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_CALL && intent.data != null) {
            callNumber = intent.data

            // make sure Simple Contacts is the default Phone app before initiating an outgoing call
            if (!isDefaultDialer()) {
                launchSetDefaultDialerIntent()
            } else {
                initOutgoingCall()
            }
        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("MissingPermission")
    private fun initOutgoingCall() {
        try {
            getHandleToUse(callNumber.toString()) { handle ->
                Bundle().apply {
                    putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, false)
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                    telecomManager.placeCall(callNumber, this)
                }
                finish()
            }
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getHandleToUse(phoneNumber: String, callback: (PhoneAccountHandle) -> Unit) {
        val defaultHandle = telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
        when {
            intent.hasExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE) -> callback(intent.getParcelableExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE)!!)
            config.getCustomSIM(phoneNumber)?.isNotEmpty() == true -> {
                val storedLabel = Uri.decode(config.getCustomSIM(phoneNumber))
                val availableSIMs = getAvailableSIMCardLabels()
                val firstornull = availableSIMs.firstOrNull { it.label == storedLabel }?.handle ?: availableSIMs.first().handle
                callback(firstornull)
            }
            defaultHandle != null -> callback(defaultHandle)
            else -> {
                handlePermission(PERMISSION_READ_PHONE_STATE) {
                    if (it) {
                        SelectSIMDialog(this, phoneNumber) { handle ->
                            callback(handle)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (!isDefaultDialer()) {
                finish()
            } else {
                initOutgoingCall()
            }
        }
    }
}
