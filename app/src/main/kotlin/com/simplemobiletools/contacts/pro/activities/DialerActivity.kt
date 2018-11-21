package com.simplemobiletools.contacts.pro.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.GsmCall
import com.simplemobiletools.contacts.pro.objects.CallManager
import kotlinx.android.synthetic.main.activity_dialer.*

// incoming call handling inspired by https://github.com/mbarrben/android_dialer_replacement
class DialerActivity : SimpleActivity(), SensorEventListener {
    private val SENSOR_SENSITIVITY = 4
    private val DISCONNECT_DELAY = 3000L

    private var number = ""
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

        if (intent.action == Intent.ACTION_CALL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            number = Uri.decode(intent.dataString).substringAfter("tel:")
            initViews()
            tryFillingOtherEndsName()
        } else if (intent.action == INCOMING_CALL && intent.extras?.containsKey(CALLER_NUMBER) == true && intent.extras?.containsKey(CALL_STATUS) == true) {
            isIncomingCall = true
            number = intent.getStringExtra(CALLER_NUMBER)
            initViews()
            updateUI(intent.getSerializableExtra(CALL_STATUS) as GsmCall.Status)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras?.containsKey(CALL_STATUS) == true) {
                updateUI(intent.getSerializableExtra(CALL_STATUS) as GsmCall.Status)
            }
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
        dialer_hangup_button.setOnClickListener {
            CallManager.declineCall()
            finish()
        }

        dialer_incoming_accept.setOnClickListener {
            CallManager.acceptCall()
        }

        dialer_incoming_decline.setOnClickListener {
            CallManager.declineCall()
            finish()
        }

        dialer_hangup_button.beVisibleIf(!isIncomingCall)
        dialer_incoming_decline.beVisibleIf(isIncomingCall)
        dialer_incoming_accept.beVisibleIf(isIncomingCall)

        dialer_label.setText(if (isIncomingCall) R.string.incoming_call else R.string.calling)
    }

    private fun tryFillingOtherEndsName() {
        ContactsHelper(this).getContactWithNumber(number) {
            runOnUiThread {
                updateOtherParticipant(it)
            }
        }
    }

    private fun updateUI(status: GsmCall.Status) {
        when (status) {
            GsmCall.Status.ACTIVE -> statusActive()
            GsmCall.Status.DISCONNECTED -> statusDisconnected()
        }
    }

    private fun statusActive() {
        isCallActive = true
        dialer_call_duration.beVisible()
        updateCallDuration()

        dialer_label.text = ""
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
        isCallActive = false
        timerHandler.removeCallbacksAndMessages(null)
        dialer_hangup_button.beGone()
        dialer_hangup_button.postDelayed({
            finish()
        }, DISCONNECT_DELAY)
    }

    private fun updateOtherParticipant(contact: Contact?) {
        if (contact != null) {
            dialer_big_name_number.text = contact.getNameToDisplay()
            dialer_number.text = number
        } else {
            dialer_big_name_number.text = number
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
