package com.simplemobiletools.contacts.pro.activities

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.models.SpeedDial

class ManageSpeedDialActivity : SimpleActivity() {
    var speedDialValues = ArrayList<SpeedDial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_speed_dial)

        val speedDialType = object : TypeToken<List<SpeedDial>>() {}.type
        speedDialValues = Gson().fromJson<ArrayList<SpeedDial>>(config.speedDial, speedDialType) ?: ArrayList(1)
    }
}
