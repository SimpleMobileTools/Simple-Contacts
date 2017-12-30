package com.simplemobiletools.contacts

import android.app.Application
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        /*if (BuildConfig.DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }*/

        checkUseEnglish()
    }
}
