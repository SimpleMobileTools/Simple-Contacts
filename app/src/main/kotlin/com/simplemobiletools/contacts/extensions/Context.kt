package com.simplemobiletools.contacts.extensions

import android.content.Context
import com.simplemobiletools.contacts.helpers.Config

val Context.config: Config get() = Config.newInstance(applicationContext)
