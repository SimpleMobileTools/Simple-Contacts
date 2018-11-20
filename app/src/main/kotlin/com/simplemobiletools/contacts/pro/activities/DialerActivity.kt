package com.simplemobiletools.contacts.pro.activities

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_dialer.*

class DialerActivity : SimpleActivity(), SensorEventListener {
    private val SENSOR_SENSITIVITY = 4
    private var number = ""
    private var sensorManager: SensorManager? = null
    private var proximity: Sensor? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
        dialer_hangup_button.setOnClickListener { finish() }
        initProximityWakeLock()

        if (intent.action == Intent.ACTION_CALL && intent.data != null && intent.dataString?.contains("tel:") == true) {
            number = Uri.decode(intent.dataString).substringAfter("tel:")
            ContactsHelper(this).getContactWithNumber(number) {
                runOnUiThread {
                    updateCallee(it)
                }
            }
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

    private fun updateCallee(contact: Contact?) {
        if (contact != null) {
            callee_big_name_number.text = contact.getNameToDisplay()
            callee_number.text = number
        } else {
            callee_big_name_number.text = number
            callee_number.beGone()
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
