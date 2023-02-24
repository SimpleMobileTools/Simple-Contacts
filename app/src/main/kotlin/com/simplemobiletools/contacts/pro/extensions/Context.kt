package com.simplemobiletools.contacts.pro.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.FileProvider
import com.simplemobiletools.commons.extensions.getCachePhoto
import com.simplemobiletools.commons.helpers.SIGNAL_PACKAGE
import com.simplemobiletools.commons.helpers.TELEGRAM_PACKAGE
import com.simplemobiletools.commons.helpers.VIBER_PACKAGE
import com.simplemobiletools.commons.helpers.WHATSAPP_PACKAGE
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.helpers.Config
import java.io.File

val Context.config: Config get() = Config.newInstance(applicationContext)
fun Context.getCachePhotoUri(file: File = getCachePhoto()) = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)

@SuppressLint("UseCompatLoadingForDrawables")
fun Context.getPackageDrawable(packageName: String): Drawable {
    return resources.getDrawable(
        when (packageName) {
            TELEGRAM_PACKAGE -> R.drawable.ic_telegram_rect_vector
            SIGNAL_PACKAGE -> R.drawable.ic_signal_rect_vector
            WHATSAPP_PACKAGE -> R.drawable.ic_whatsapp_rect_vector
            VIBER_PACKAGE -> R.drawable.ic_viber_rect_vector
            else -> R.drawable.ic_threema_rect_vector
        },
        theme
    )
}
