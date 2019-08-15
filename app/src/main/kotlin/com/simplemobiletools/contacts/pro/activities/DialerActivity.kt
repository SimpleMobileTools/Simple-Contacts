package com.simplemobiletools.contacts.pro.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.view.Menu
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.isDefaultDialer
import com.simplemobiletools.contacts.pro.extensions.telecomManager
import com.simplemobiletools.contacts.pro.helpers.REQUEST_CODE_SET_DEFAULT_DIALER

@TargetApi(Build.VERSION_CODES.M)
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
            Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL))
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, false)
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                telecomManager.placeCall(callNumber, this)
                finish()
            }
        } catch (e: Exception) {
            showErrorToast(e)
            finish()
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
