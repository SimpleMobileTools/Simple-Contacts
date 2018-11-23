package com.simplemobiletools.contacts.pro.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.objects.CallManager
import com.simplemobiletools.contacts.pro.services.DialerCallService
import kotlinx.android.synthetic.main.activity_dialer.*

// incoming call handling inspired by https://github.com/mbarrben/android_dialer_replacement
@TargetApi(Build.VERSION_CODES.M)
class DialerActivity : SimpleActivity(), SensorEventListener {
    private val SENSOR_SENSITIVITY = 4
    private val DISCONNECT_DELAY = 3000L

    private var callNumber = ""
    private var callStatus = Call.STATE_NEW
    private var isIncomingCall = false
    private var isCallActive = false
    private var callDuration = 0
    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var timerHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
        initProximityWakeLock()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(messageReceiver, IntentFilter(DIALER_INTENT_FILTER))

        val action = intent.action
        val extras = intent.extras
        if (extras?.getBoolean(ANSWER_CALL, false) == true) {
            callNumber = intent.getStringExtra(CALL_NUMBER)
            initViews()
            CallManager.answerCall()
            tryFillingOtherEndsName()
        } else if (action == Intent.ACTION_CALL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            callNumber = Uri.decode(intent.dataString).substringAfter("tel:")
            initViews()
            tryFillingOtherEndsName()
            initOutgoingCall()
        } else if (action == INCOMING_CALL && extras?.containsKey(CALL_NUMBER) == true && extras.containsKey(CALL_STATUS)) {
            isIncomingCall = true
            callNumber = intent.getStringExtra(CALL_NUMBER)
            initViews()
            updateUI(intent.getIntExtra(CALL_STATUS, Call.STATE_NEW))
            tryFillingOtherEndsName()
        } else if (action == RESUME_DIALER && extras?.containsKey(CALL_NUMBER) == true && extras.containsKey(CALL_STATUS) && extras.containsKey(IS_INCOMING_CALL)) {
            callNumber = intent.getStringExtra(CALL_NUMBER)
            callStatus = intent.getIntExtra(CALL_STATUS, Call.STATE_NEW)
            isIncomingCall = intent.getBooleanExtra(IS_INCOMING_CALL, false)
            initViews()
            updateUI(callStatus)
            tryFillingOtherEndsName()
        } else {
            toast(R.string.unknown_error_occurred)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(dialer_holder)
        sensorManager!!.registerListener(this, proximity!!, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
        if (!isCallActive && callStatus != Call.STATE_DISCONNECTED) {
            startNotificationService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras?.containsKey(CALL_STATUS) == true) {
                updateUI(intent.getIntExtra(CALL_STATUS, Call.STATE_NEW))
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (!isCallActive && !isIncomingCall) {
            hangUp()
        }
    }

    private fun initProximityWakeLock() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        proximityWakeLock = if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SimpleContacts:ProximityWakeLock")
        } else {
            null
        }
    }

    private fun initViews() {
        dialer_hangup_button.setOnClickListener { hangUp() }
        dialer_incoming_accept.setOnClickListener { CallManager.answerCall() }
        dialer_incoming_decline.setOnClickListener { hangUp() }

        dialer_hangup_button.beVisibleIf(!isIncomingCall)
        dialer_incoming_decline.beVisibleIf(isIncomingCall)
        dialer_incoming_accept.beVisibleIf(isIncomingCall)

        dialer_label.setText(if (isIncomingCall) R.string.incoming_call_from else R.string.calling)
    }

    private fun initOutgoingCall() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.fromParts("tel:", callNumber, null)
        val extras = Bundle()
        telecomManager.placeCall(uri, extras)
    }

    private fun startNotificationService() {
        Intent(this, DialerCallService::class.java).apply {
            putExtra(CALL_NUMBER, callNumber)
            putExtra(CALL_STATUS, callStatus)
            putExtra(IS_INCOMING_CALL, isIncomingCall)
            startService(this)
        }
    }

    private fun stopNotificationService() {
        Intent(this, DialerCallService::class.java).apply {
            stopService(this)
        }
    }

    private fun hangUp() {
        callStatus = Call.STATE_DISCONNECTED
        stopNotificationService()
        CallManager.declineCall()
        finish()
    }

    private fun tryFillingOtherEndsName() {
        ContactsHelper(this).getContactWithNumber(callNumber) {
            runOnUiThread {
                updateOtherParticipant(it)
            }
        }
    }

    private fun updateUI(status: Int) {
        callStatus = status
        when (status) {
            Call.STATE_ACTIVE -> statusActive()
            Call.STATE_DISCONNECTED -> statusDisconnected()
        }
    }

    private fun statusActive() {
        startNotificationService()
        isCallActive = true
        dialer_call_duration.beVisible()
        updateCallDuration()

        dialer_label.setText(R.string.ongoing_call)
        dialer_hangup_button.beVisible()
        dialer_incoming_accept.beGone()
        dialer_incoming_decline.beGone()
    }

    private fun updateCallDuration() {
        dialer_call_duration.text = callDuration.getFormattedDuration()
        timerHandler.postDelayed({
            if (isCallActive) {
                callDuration++
                updateCallDuration()
            }
        }, 1000)
    }

    private fun statusDisconnected() {
        stopNotificationService()
        timerHandler.removeCallbacksAndMessages(null)
        dialer_hangup_button.beGone()
        dialer_label.setText(R.string.disconnected)
        if (isCallActive) {
            dialer_hangup_button.postDelayed({
                finish()
            }, DISCONNECT_DELAY)
        } else {
            finish()
        }
        isCallActive = false
    }

    private fun updateOtherParticipant(contact: Contact?) {
        if (contact != null) {
            dialer_big_name_number.text = contact.getNameToDisplay()
            dialer_number.text = callNumber
        } else {
            dialer_big_name_number.text = callNumber
            dialer_number.beGone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                turnOffScreen()
            } else {
                turnOnScreen()
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun turnOffScreen() {
        if (proximityWakeLock?.isHeld == false) {
            proximityWakeLock!!.acquire()
        }
    }

    private fun turnOnScreen() {
        if (proximityWakeLock?.isHeld == true) {
            proximityWakeLock!!.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
        }
    }
}
