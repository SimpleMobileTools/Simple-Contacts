package com.simplemobiletools.contacts.pro.extensions

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.biometrics.BiometricManager
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.FileProvider
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.receivers.AutomaticBackupReceiver
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.BaseColumns
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract.CommonDataKinds.BaseTypes
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.DocumentsContract
import android.provider.MediaStore.*
import android.provider.OpenableColumns
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
//import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.exifinterface.media.ExifInterface
import androidx.loader.content.CursorLoader
import com.github.ajalt.reprint.core.Reprint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.BlockedNumber
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.helpers.ContactsHelper
import com.simplemobiletools.contacts.pro.helpers.KEY_MAILTO
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.toInt

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
        }, theme
    )
}

fun Context.getAutomaticBackupIntent(): PendingIntent {
    val intent = Intent(this, AutomaticBackupReceiver::class.java)
    return PendingIntent.getBroadcast(this, AUTOMATIC_BACKUP_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.scheduleNextAutomaticBackup() {
    if (config.autoBackup) {
        val backupAtMillis = getNextAutoBackupTime().millis
        val pendingIntent = getAutomaticBackupIntent()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, backupAtMillis, pendingIntent)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

fun Context.cancelScheduledAutomaticBackup() {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(getAutomaticBackupIntent())
}

fun Context.checkAndBackupContactsOnBoot() {
    if (config.autoBackup) {
        val previousRealBackupTime = config.lastAutoBackupTime
        val previousScheduledBackupTime = getPreviousAutoBackupTime().millis
        val missedPreviousBackup = previousRealBackupTime < previousScheduledBackupTime
        if (missedPreviousBackup) {
            // device was probably off at the scheduled time so backup now
            backupContacts()
        }
    }
}

fun Context.backupContacts() {
    require(isRPlus())
    ensureBackgroundThread {
        val config = config
        ContactsHelper(this).getContactsToExport(selectedContactSources = config.autoBackupContactSources) { contactsToBackup ->
            if (contactsToBackup.isEmpty()) {
                toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
                config.lastAutoBackupTime = DateTime.now().millis
                scheduleNextAutomaticBackup()
                return@getContactsToExport
            }


            val now = DateTime.now()
            val year = now.year.toString()
            val month = now.monthOfYear.ensureTwoDigits()
            val day = now.dayOfMonth.ensureTwoDigits()
            val hours = now.hourOfDay.ensureTwoDigits()
            val minutes = now.minuteOfHour.ensureTwoDigits()
            val seconds = now.secondOfMinute.ensureTwoDigits()

            val filename = config.autoBackupFilename
                .replace("%Y", year, false)
                .replace("%M", month, false)
                .replace("%D", day, false)
                .replace("%h", hours, false)
                .replace("%m", minutes, false)
                .replace("%s", seconds, false)

            val outputFolder = File(config.autoBackupFolder).apply {
                mkdirs()
            }

            var exportFile = File(outputFolder, "$filename.vcf")
            var exportFilePath = exportFile.absolutePath
            val outputStream = try {
                if (hasProperStoredFirstParentUri(exportFilePath)) {
                    val exportFileUri = createDocumentUriUsingFirstParentTreeUri(exportFilePath)
                    if (!getDoesFilePathExist(exportFilePath)) {
                        createSAFFileSdk30(exportFilePath)
                    }
                    applicationContext.contentResolver.openOutputStream(exportFileUri, "wt") ?: FileOutputStream(exportFile)
                } else {
                    var num = 0
                    while (getDoesFilePathExist(exportFilePath) && !exportFile.canWrite()) {
                        num++
                        exportFile = File(outputFolder, "${filename}_${num}.vcf")
                        exportFilePath = exportFile.absolutePath
                    }
                    FileOutputStream(exportFile)
                }
            } catch (e: Exception) {
                showErrorToast(e)
                scheduleNextAutomaticBackup()
                return@getContactsToExport
            }

            val exportResult = try {
                ContactsHelper(this).exportContacts(contactsToBackup, outputStream)
            } catch (e: Exception) {
                showErrorToast(e)
            }

            when (exportResult) {
                ExportResult.EXPORT_OK -> toast(com.simplemobiletools.commons.R.string.exporting_successful)
                else -> toast(com.simplemobiletools.commons.R.string.exporting_failed)
            }

            config.lastAutoBackupTime = DateTime.now().millis
            scheduleNextAutomaticBackup()
        }
    }
}

fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

val Context.isRTLLayout: Boolean get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

val Context.areSystemAnimationsEnabled: Boolean get() = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f) > 0f

fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
    }
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}

fun Context.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(com.simplemobiletools.commons.R.string.error), msg), length)
}

fun Context.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)
val Context.sdCardPath: String get() = baseConfig.sdCardPath
val Context.internalStoragePath: String get() = baseConfig.internalStoragePath
val Context.otgPath: String get() = baseConfig.OTGPath

fun Context.isFingerPrintSensorAvailable() = Reprint.isHardwarePresent()

fun Context.getLatestMediaId(uri: Uri = Files.getContentUri("external")): Long {
    val projection = arrayOf(
        BaseColumns._ID
    )
    try {
        val cursor = queryCursorDesc(uri, projection, BaseColumns._ID, 1)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(BaseColumns._ID)
            }
        }
    } catch (ignored: Exception) {
    }
    return 0
}

private fun Context.queryCursorDesc(
    uri: Uri,
    projection: Array<String>,
    sortColumn: String,
    limit: Int,
): Cursor? {
    return if (isRPlus()) {
        val queryArgs = bundleOf(
            ContentResolver.QUERY_ARG_LIMIT to limit,
            ContentResolver.QUERY_ARG_SORT_DIRECTION to ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            ContentResolver.QUERY_ARG_SORT_COLUMNS to arrayOf(sortColumn),
        )
        contentResolver.query(uri, projection, queryArgs, null)
    } else {
        val sortOrder = "$sortColumn DESC LIMIT $limit"
        contentResolver.query(uri, projection, null, null, sortOrder)
    }
}

fun Context.getLatestMediaByDateId(uri: Uri = Files.getContentUri("external")): Long {
    val projection = arrayOf(
        BaseColumns._ID
    )
    try {
        val cursor = queryCursorDesc(uri, projection, Images.ImageColumns.DATE_TAKEN, 1)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(BaseColumns._ID)
            }
        }
    } catch (ignored: Exception) {
    }
    return 0
}

// some helper functions were taken from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    if (isDownloadsDocument(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        if (id.areDigitsOnly()) {
            val newUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
            val path = getDataColumn(newUri)
            if (path != null) {
                return path
            }
        }
    } else if (isExternalStorageDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val parts = documentId.split(":")
        if (parts[0].equals("primary", true)) {
            return "${Environment.getExternalStorageDirectory().absolutePath}/${parts[1]}"
        }
    } else if (isMediaDocument(uri)) {
        val documentId = DocumentsContract.getDocumentId(uri)
        val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        val type = split[0]

        val contentUri = when (type) {
            "video" -> Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> Audio.Media.EXTERNAL_CONTENT_URI
            else -> Images.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "_id=?"
        val selectionArgs = arrayOf(split[1])
        val path = getDataColumn(contentUri, selection, selectionArgs)
        if (path != null) {
            return path
        }
    }

    return getDataColumn(uri)
}

fun Context.getDataColumn(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
    try {
        val projection = arrayOf(Files.FileColumns.DATA)
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val data = cursor.getStringValue(Files.FileColumns.DATA)
                if (data != "null") {
                    return data
                }
            }
        }
    } catch (e: Exception) {
    }
    return null
}

private fun isMediaDocument(uri: Uri) = uri.authority == "com.android.providers.media.documents"

private fun isDownloadsDocument(uri: Uri) = uri.authority == "com.android.providers.downloads.documents"

private fun isExternalStorageDocument(uri: Uri) = uri.authority == "com.android.externalstorage.documents"

fun Context.hasPermission(permId: Int) = ContextCompat.checkSelfPermission(this, getPermissionString(permId)) == PackageManager.PERMISSION_GRANTED

fun Context.hasAllPermissions(permIds: Collection<Int>) = permIds.all(this::hasPermission)

fun Context.getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
    PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
    PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
    PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
    PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
    PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
    PERMISSION_MEDIA_LOCATION -> if (isQPlus()) Manifest.permission.ACCESS_MEDIA_LOCATION else ""
    PERMISSION_POST_NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
    PERMISSION_READ_MEDIA_IMAGES -> Manifest.permission.READ_MEDIA_IMAGES
    PERMISSION_READ_MEDIA_VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
    PERMISSION_READ_MEDIA_AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
    PERMISSION_ACCESS_COARSE_LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
    PERMISSION_ACCESS_FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    PERMISSION_READ_MEDIA_VISUAL_USER_SELECTED -> Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    PERMISSION_READ_SYNC_SETTINGS -> Manifest.permission.READ_SYNC_SETTINGS
    else -> ""
}

fun Context.launchActivityIntent(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(com.simplemobiletools.commons.R.string.no_app_found)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.simplemobiletools.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    var uri = if (file.isMediaFile()) {
        getMediaContentUri(file.absolutePath)
    } else {
        getMediaContent(file.absolutePath, Files.getContentUri("external"))
    }

    if (uri == null) {
        uri = FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }

    return uri!!
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> Video.Media.EXTERNAL_CONTENT_URI
        else -> Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(Images.Media._ID)
    val selection = Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                val id = cursor.getIntValue(Images.Media._ID).toString()
                return Uri.withAppendedPath(uri, id)
            }
        }
    } catch (e: Exception) {
    }
    return null
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    queryArgs: Bundle,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, queryArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.getFilenameFromUri(uri: Uri): String {
    return if (uri.scheme == "file") {
        File(uri.toString()).name
    } else {
        getFilenameFromContentUri(uri) ?: uri.lastPathSegment ?: ""
    }
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path?.getMimeType() ?: ""
    if (mimetype.isEmpty()) {
        try {
            mimetype = contentResolver.getType(uri) ?: ""
        } catch (e: IllegalStateException) {
        }
    }
    return mimetype
}

fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    return when {
        hasProperStoredAndroidTreeUri(path) && isRestrictedSAFOnlyRoot(path) -> {
            getAndroidSAFUri(path)
        }

        hasProperStoredDocumentUriSdk30(path) && isAccessibleWithSAFSdk30(path) -> {
            createDocumentUriUsingFirstParentTreeUri(path)
        }

        isPathOnOTG(path) -> {
            getDocumentFile(path)?.uri
        }

        else -> {
            val uri = Uri.parse(path)
            if (uri.scheme == "content") {
                uri
            } else {
                val newPath = if (uri.toString().startsWith("/")) uri.toString() else uri.path
                val file = File(newPath)
                getFilePublicUri(file, applicationId)
            }
        }
    }
}

fun Context.ensurePublicUri(uri: Uri, applicationId: String): Uri {
    return if (uri.scheme == "content") {
        uri
    } else {
        val file = File(uri.path)
        getFilePublicUri(file, applicationId)
    }
}

fun Context.getFilenameFromContentUri(uri: Uri): String? {
    val projection = arrayOf(
        OpenableColumns.DISPLAY_NAME
    )

    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
            }
        }
    } catch (e: Exception) {
    }
    return null
}

fun Context.getSizeFromContentUri(uri: Uri): Long {
    val projection = arrayOf(OpenableColumns.SIZE)
    try {
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(OpenableColumns.SIZE)
            }
        }
    } catch (e: Exception) {
    }
    return 0L
}

fun Context.getMyContentProviderCursorLoader() = CursorLoader(this, MyContentProvider.MY_CONTENT_URI, null, null, null, null)

fun Context.getMyContactsCursor(favoritesOnly: Boolean, withPhoneNumbersOnly: Boolean) = try {
    val getFavoritesOnly = if (favoritesOnly) "1" else "0"
    val getWithPhoneNumbersOnly = if (withPhoneNumbersOnly) "1" else "0"
    val args = arrayOf(getFavoritesOnly, getWithPhoneNumbersOnly)
    CursorLoader(this, MyContactsContentProvider.CONTACTS_CONTENT_URI, null, null, args, null).loadInBackground()
} catch (e: Exception) {
    null
}

fun Context.getCurrentFormattedDateTime(): String {
    val simpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
    return simpleDateFormat.format(Date(System.currentTimeMillis()))
}

fun Context.updateSDCardPath() {
    ensureBackgroundThread {
        val oldPath = baseConfig.sdCardPath
        baseConfig.sdCardPath = getSDCardPath()
        if (oldPath != baseConfig.sdCardPath) {
            baseConfig.sdTreeUri = ""
        }
    }
}

fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}

fun Context.isThankYouInstalled() = isPackageInstalled("com.simplemobiletools.thankyou")

fun Context.isOrWasThankYouInstalled(): Boolean {
    return when {
        resources.getBoolean(com.simplemobiletools.commons.R.bool.pretend_thank_you_installed) -> true
        baseConfig.hadThankYouInstalled -> true
        isThankYouInstalled() -> {
            baseConfig.hadThankYouInstalled = true
            true
        }

        else -> false
    }
}

fun Context.isAProApp() = packageName.startsWith("com.simplemobiletools.") && packageName.removeSuffix(".debug").endsWith(".pro")

fun Context.getCustomizeColorsString(): String {
    val textId = if (isOrWasThankYouInstalled()) {
        com.simplemobiletools.commons.R.string.customize_colors
    } else {
        com.simplemobiletools.commons.R.string.customize_colors_locked
    }

    return getString(textId)
}

fun Context.addLockedLabelIfNeeded(stringId: Int): String {
    return if (isOrWasThankYouInstalled()) {
        getString(stringId)
    } else {
        "${getString(stringId)} (${getString(com.simplemobiletools.commons.R.string.feature_locked)})"
    }
}

fun Context.isPackageInstalled(pkgName: String): Boolean {
    return try {
        packageManager.getPackageInfo(pkgName, 0)
        true
    } catch (e: Exception) {
        false
    }
}

// format day bits to strings like "Mon, Tue, Wed"
fun Context.getSelectedDaysString(bitMask: Int): String {
    val dayBits = arrayListOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT)
    val weekDays = resources.getStringArray(com.simplemobiletools.commons.R.array.week_days_short).toList() as ArrayList<String>

    if (baseConfig.isSundayFirst) {
        dayBits.moveLastItemToFront()
        weekDays.moveLastItemToFront()
    }

    var days = ""
    dayBits.forEachIndexed { index, bit ->
        if (bitMask and bit != 0) {
            days += "${weekDays[index]}, "
        }
    }
    return days.trim().trimEnd(',')
}

fun Context.formatMinutesToTimeString(totalMinutes: Int) = formatSecondsToTimeString(totalMinutes * 60)

fun Context.formatSecondsToTimeString(totalSeconds: Int): String {
    val days = totalSeconds / DAY_SECONDS
    val hours = (totalSeconds % DAY_SECONDS) / HOUR_SECONDS
    val minutes = (totalSeconds % HOUR_SECONDS) / MINUTE_SECONDS
    val seconds = totalSeconds % MINUTE_SECONDS
    val timesString = StringBuilder()
    if (days > 0) {
        val daysString = String.format(resources.getQuantityString(com.simplemobiletools.commons.R.plurals.days, days, days))
        timesString.append("$daysString, ")
    }

    if (hours > 0) {
        val hoursString = String.format(resources.getQuantityString(com.simplemobiletools.commons.R.plurals.hours, hours, hours))
        timesString.append("$hoursString, ")
    }

    if (minutes > 0) {
        val minutesString = String.format(resources.getQuantityString(com.simplemobiletools.commons.R.plurals.minutes, minutes, minutes))
        timesString.append("$minutesString, ")
    }

    if (seconds > 0) {
        val secondsString = String.format(resources.getQuantityString(com.simplemobiletools.commons.R.plurals.seconds, seconds, seconds))
        timesString.append(secondsString)
    }

    var result = timesString.toString().trim().trimEnd(',')
    if (result.isEmpty()) {
        result = String.format(resources.getQuantityString(com.simplemobiletools.commons.R.plurals.minutes, 0, 0))
    }
    return result
}

fun Context.getFormattedMinutes(minutes: Int, showBefore: Boolean = true) = getFormattedSeconds(if (minutes == -1) minutes else minutes * 60, showBefore)

fun Context.getFormattedSeconds(seconds: Int, showBefore: Boolean = true) = when (seconds) {
    -1 -> getString(com.simplemobiletools.commons.R.string.no_reminder)
    0 -> getString(com.simplemobiletools.commons.R.string.at_start)
    else -> {
        when {
            seconds < 0 && seconds > -60 * 60 * 24 -> {
                val minutes = -seconds / 60
                getString(com.simplemobiletools.commons.R.string.during_day_at).format(minutes / 60, minutes % 60)
            }

            seconds % YEAR_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.years_before else com.simplemobiletools.commons.R.plurals.by_years
                resources.getQuantityString(base, seconds / YEAR_SECONDS, seconds / YEAR_SECONDS)
            }

            seconds % MONTH_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.months_before else com.simplemobiletools.commons.R.plurals.by_months
                resources.getQuantityString(base, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            }

            seconds % WEEK_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.weeks_before else com.simplemobiletools.commons.R.plurals.by_weeks
                resources.getQuantityString(base, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            }

            seconds % DAY_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.days_before else com.simplemobiletools.commons.R.plurals.by_days
                resources.getQuantityString(base, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
            }

            seconds % HOUR_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.hours_before else com.simplemobiletools.commons.R.plurals.by_hours
                resources.getQuantityString(base, seconds / HOUR_SECONDS, seconds / HOUR_SECONDS)
            }

            seconds % MINUTE_SECONDS == 0 -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.minutes_before else com.simplemobiletools.commons.R.plurals.by_minutes
                resources.getQuantityString(base, seconds / MINUTE_SECONDS, seconds / MINUTE_SECONDS)
            }

            else -> {
                val base = if (showBefore) com.simplemobiletools.commons.R.plurals.seconds_before else com.simplemobiletools.commons.R.plurals.by_seconds
                resources.getQuantityString(base, seconds, seconds)
            }
        }
    }
}

fun Context.getDefaultAlarmTitle(type: Int): String {
    val alarmString = getString(com.simplemobiletools.commons.R.string.alarm)
    return try {
        RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(type))?.getTitle(this) ?: alarmString
    } catch (e: Exception) {
        alarmString
    }
}

fun Context.getDefaultAlarmSound(type: Int) = AlarmSound(0, getDefaultAlarmTitle(type), RingtoneManager.getDefaultUri(type).toString())

fun Context.grantReadUriPermission(uriString: String) {
    try {
        // ensure custom reminder sounds play well
        grantUriPermission("com.android.systemui", Uri.parse(uriString), Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (ignored: Exception) {
    }
}

fun Context.storeNewYourAlarmSound(resultData: Intent): AlarmSound {
    val uri = resultData.data
    var filename = getFilenameFromUri(uri!!)
    if (filename.isEmpty()) {
        filename = getString(com.simplemobiletools.commons.R.string.alarm)
    }

    val token = object : TypeToken<ArrayList<AlarmSound>>() {}.type
    val yourAlarmSounds = Gson().fromJson<ArrayList<AlarmSound>>(baseConfig.yourAlarmSounds, token)
        ?: ArrayList()
    val newAlarmSoundId = (yourAlarmSounds.maxByOrNull { it.id }?.id ?: YOUR_ALARM_SOUNDS_MIN_ID) + 1
    val newAlarmSound = AlarmSound(newAlarmSoundId, filename, uri.toString())
    if (yourAlarmSounds.firstOrNull { it.uri == uri.toString() } == null) {
        yourAlarmSounds.add(newAlarmSound)
    }

    baseConfig.yourAlarmSounds = Gson().toJson(yourAlarmSounds)

    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, takeFlags)

    return newAlarmSound
}

@RequiresApi(Build.VERSION_CODES.N)
fun Context.saveImageRotation(path: String, degrees: Int): Boolean {
    if (!needsStupidWritePermissions(path)) {
        saveExifRotation(ExifInterface(path), degrees)
        return true
    } else if (isNougatPlus()) {
        val documentFile = getSomeDocumentFile(path)
        if (documentFile != null) {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(documentFile.uri, "rw")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            saveExifRotation(ExifInterface(fileDescriptor), degrees)
            return true
        }
    }
    return false
}

fun Context.saveExifRotation(exif: ExifInterface, degrees: Int) {
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val orientationDegrees = (orientation.degreesFromOrientation() + degrees) % 360
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientationDegrees.orientationFromDegrees())
    exif.saveAttributes()
}

fun Context.getLaunchIntent() = packageManager.getLaunchIntentForPackage(baseConfig.appId)

fun Context.getCanAppBeUpgraded() = proPackages.contains(baseConfig.appId.removeSuffix(".debug").removePrefix("com.simplemobiletools."))

fun Context.getProUrl() = "https://play.google.com/store/apps/details?id=${baseConfig.appId.removeSuffix(".debug")}.pro"

fun Context.getStoreUrl() = "https://play.google.com/store/apps/details?id=${packageName.removeSuffix(".debug")}"

fun Context.getTimeFormat() = if (baseConfig.use24HourFormat) TIME_FORMAT_24 else TIME_FORMAT_12

fun Context.getResolution(path: String): Point? {
    return if (path.isImageFast() || path.isImageSlow()) {
        getImageResolution(path)
    } else if (path.isVideoFast() || path.isVideoSlow()) {
        getVideoResolution(path)
    } else {
        null
    }
}

fun Context.getImageResolution(path: String): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    if (isRestrictedSAFOnlyRoot(path)) {
        BitmapFactory.decodeStream(contentResolver.openInputStream(getAndroidSAFUri(path)), null, options)
    } else {
        BitmapFactory.decodeFile(path, options)
    }

    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}

fun Context.getVideoResolution(path: String): Point? {
    var point = try {
        val retriever = MediaMetadataRetriever()
        if (isRestrictedSAFOnlyRoot(path)) {
            retriever.setDataSource(this, getAndroidSAFUri(path))
        } else {
            retriever.setDataSource(path)
        }

        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }

    if (point == null && path.startsWith("content://", true)) {
        try {
            val fd = contentResolver.openFileDescriptor(Uri.parse(path), "r")?.fileDescriptor
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(fd)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
            point = Point(width, height)
        } catch (ignored: Exception) {
        }
    }

    return point
}

fun Context.getDuration(path: String): Int? {
    val projection = arrayOf(
        MediaColumns.DURATION
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return Math.round(cursor.getIntValue(MediaColumns.DURATION) / 1000.toDouble()).toInt()
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt() / 1000f)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getTitle(path: String): String? {
    val projection = arrayOf(
        MediaColumns.TITLE
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(MediaColumns.TITLE)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getArtist(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ARTIST
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ARTIST)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getAlbum(path: String): String? {
    val projection = arrayOf(
        Audio.Media.ALBUM
    )

    val uri = getFileUri(path)
    val selection = if (path.startsWith("content://")) "${BaseColumns._ID} = ?" else "${MediaColumns.DATA} = ?"
    val selectionArgs = if (path.startsWith("content://")) arrayOf(path.substringAfterLast("/")) else arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getStringValue(Audio.Media.ALBUM)
            }
        }
    } catch (ignored: Exception) {
    }

    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
    } catch (ignored: Exception) {
        null
    }
}

fun Context.getMediaStoreLastModified(path: String): Long {
    val projection = arrayOf(
        MediaColumns.DATE_MODIFIED
    )

    val uri = getFileUri(path)
    val selection = "${BaseColumns._ID} = ?"
    val selectionArgs = arrayOf(path.substringAfterLast("/"))

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(MediaColumns.DATE_MODIFIED) * 1000
            }
        }
    } catch (ignored: Exception) {
    }
    return 0
}

fun Context.getStringsPackageName() = getString(com.simplemobiletools.commons.R.string.package_name)

fun Context.getFontSizeText() = getString(
    when (baseConfig.fontSize) {
        FONT_SIZE_SMALL -> com.simplemobiletools.commons.R.string.small
        FONT_SIZE_MEDIUM -> com.simplemobiletools.commons.R.string.medium
        FONT_SIZE_LARGE -> com.simplemobiletools.commons.R.string.large
        else -> com.simplemobiletools.commons.R.string.extra_large
    }
)

fun Context.getTextSize() = when (baseConfig.fontSize) {
    FONT_SIZE_SMALL -> resources.getDimension(com.simplemobiletools.commons.R.dimen.smaller_text_size)
    FONT_SIZE_MEDIUM -> resources.getDimension(com.simplemobiletools.commons.R.dimen.bigger_text_size)
    FONT_SIZE_LARGE -> resources.getDimension(com.simplemobiletools.commons.R.dimen.big_text_size)
    else -> resources.getDimension(com.simplemobiletools.commons.R.dimen.extra_big_text_size)
}

val Context.telecomManager: TelecomManager get() = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.navigationBarOnSide: Boolean get() = usableScreenSize.x < realScreenSize.x && usableScreenSize.x > usableScreenSize.y
val Context.navigationBarOnBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarOnBottom && navigationBarSize.y != usableScreenSize.y) navigationBarSize.y else 0
val Context.navigationBarWidth: Int get() = if (navigationBarOnSide) navigationBarSize.x else 0

val Context.navigationBarSize: Point
    get() = when {
        navigationBarOnSide -> Point(newNavigationBarHeight, usableScreenSize.y)
        navigationBarOnBottom -> Point(usableScreenSize.x, newNavigationBarHeight)
        else -> Point()
    }

val Context.newNavigationBarHeight: Int
    get() {
        var navigationBarHeight = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

val Context.statusBarHeight: Int
    get() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

val Context.actionBarHeight: Int
    get() {
        val styledAttributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = styledAttributes.getDimension(0, 0f)
        styledAttributes.recycle()
        return actionBarHeight.toInt()
    }

val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }

fun Context.isUsingGestureNavigation(): Boolean {
    return try {
        val resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        if (resourceId > 0) {
            resources.getInteger(resourceId) == 2
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

fun Context.getCornerRadius() = resources.getDimension(com.simplemobiletools.commons.R.dimen.rounded_corner_radius_small)

// we need the Default Dialer functionality only in Simple Dialer and in Simple Contacts for now
fun Context.isDefaultDialer(): Boolean {
    return if (!packageName.startsWith("com.simplemobiletools.contacts") && !packageName.startsWith("com.simplemobiletools.dialer")) {
        true
    } else if ((packageName.startsWith("com.simplemobiletools.contacts") || packageName.startsWith("com.simplemobiletools.dialer")) && isQPlus()) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        telecomManager.defaultDialerPackage == packageName
    }
}

fun Context.getContactsHasMap(callback: (HashMap<String, String>) -> Unit) {
    ContactsHelper(this).getContacts(showOnlyContactsWithNumbers = true) { contactList ->
        val privateContacts: HashMap<String, String> = HashMap()
        for (contact in contactList) {
            for (phoneNumber in contact.phoneNumbers) {
                privateContacts[PhoneNumberUtils.stripSeparators(phoneNumber.value)] = contact.name
            }
        }
        callback(privateContacts)
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.getBlockedNumbersWithContact(callback: (ArrayList<BlockedNumber>) -> Unit) {
    getContactsHasMap { contacts ->
        val blockedNumbers = ArrayList<BlockedNumber>()
        if (!isNougatPlus() || !isDefaultDialer()) {
            callback(blockedNumbers)
        }

        val uri = BlockedNumbers.CONTENT_URI
        val projection = arrayOf(
            BlockedNumbers.COLUMN_ID,
            BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
            BlockedNumbers.COLUMN_E164_NUMBER,
        )

        queryCursor(uri, projection) { cursor ->
            val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
            val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER) ?: ""
            val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER) ?: number
            val comparableNumber = normalizedNumber.trimToComparableNumber()

            val contactName = contacts[number]
            val blockedNumber = BlockedNumber(id, number, normalizedNumber, comparableNumber, contactName)
            blockedNumbers.add(blockedNumber)
        }

        val blockedNumbersPair = blockedNumbers.partition { it.contactName != null }
        val blockedNumbersWithNameSorted = blockedNumbersPair.first.sortedBy { it.contactName }
        val blockedNumbersNoNameSorted = blockedNumbersPair.second.sortedBy { it.number }

        callback(ArrayList(blockedNumbersWithNameSorted + blockedNumbersNoNameSorted))
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.getBlockedNumbers(): ArrayList<BlockedNumber> {
    val blockedNumbers = ArrayList<BlockedNumber>()
    if (!isNougatPlus() || !isDefaultDialer()) {
        return blockedNumbers
    }

    val uri = BlockedNumbers.CONTENT_URI
    val projection = arrayOf(
        BlockedNumbers.COLUMN_ID,
        BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
        BlockedNumbers.COLUMN_E164_NUMBER
    )

    queryCursor(uri, projection) { cursor ->
        val id = cursor.getLongValue(BlockedNumbers.COLUMN_ID)
        val number = cursor.getStringValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER) ?: ""
        val normalizedNumber = cursor.getStringValue(BlockedNumbers.COLUMN_E164_NUMBER) ?: number
        val comparableNumber = normalizedNumber.trimToComparableNumber()
        val blockedNumber = BlockedNumber(id, number, normalizedNumber, comparableNumber)
        blockedNumbers.add(blockedNumber)
    }

    return blockedNumbers
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.addBlockedNumber(number: String): Boolean {
    ContentValues().apply {
        put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
        if (number.isPhoneNumber()) {
            put(BlockedNumbers.COLUMN_E164_NUMBER, PhoneNumberUtils.normalizeNumber(number))
        }
        try {
            contentResolver.insert(BlockedNumbers.CONTENT_URI, this)
        } catch (e: Exception) {
            showErrorToast(e)
            return false
        }
    }
    return true
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.deleteBlockedNumber(number: String): Boolean {
    val selection = "${BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
    val selectionArgs = arrayOf(number)

    return if (isNumberBlocked(number)) {
        val deletedRowCount = contentResolver.delete(BlockedNumbers.CONTENT_URI, selection, selectionArgs)

        deletedRowCount > 0
    } else {
        true
    }
}

fun Context.isNumberBlocked(number: String, blockedNumbers: ArrayList<BlockedNumber> = getBlockedNumbers()): Boolean {
    if (!isNougatPlus()) {
        return false
    }

    val numberToCompare = number.trimToComparableNumber()

    return blockedNumbers.any {
        numberToCompare == it.numberToCompare ||
            numberToCompare == it.number ||
            PhoneNumberUtils.stripSeparators(number) == it.number
    } || isNumberBlockedByPattern(number, blockedNumbers)
}

fun Context.isNumberBlockedByPattern(number: String, blockedNumbers: ArrayList<BlockedNumber> = getBlockedNumbers()): Boolean {
    for (blockedNumber in blockedNumbers) {
        val num = blockedNumber.number
        if (num.isBlockedNumberPattern()) {
            val pattern = num.replace("+", "\\+").replace("*", ".*")
            if (number.matches(pattern.toRegex())) {
                return true
            }
        }
    }
    return false
}

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(com.simplemobiletools.commons.R.string.simple_commons), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    val toastText = String.format(getString(com.simplemobiletools.commons.R.string.value_copied_to_clipboard_show), text)
    toast(toastText)
}

fun Context.getPhoneNumberTypeText(type: Int, label: String): String {
    return if (type == BaseTypes.TYPE_CUSTOM) {
        label
    } else {
        getString(
            when (type) {
                Phone.TYPE_MOBILE -> com.simplemobiletools.commons.R.string.mobile
                Phone.TYPE_HOME -> com.simplemobiletools.commons.R.string.home
                Phone.TYPE_WORK -> com.simplemobiletools.commons.R.string.work
                Phone.TYPE_MAIN -> com.simplemobiletools.commons.R.string.main_number
                Phone.TYPE_FAX_WORK -> com.simplemobiletools.commons.R.string.work_fax
                Phone.TYPE_FAX_HOME -> com.simplemobiletools.commons.R.string.home_fax
                Phone.TYPE_PAGER -> com.simplemobiletools.commons.R.string.pager
                else -> com.simplemobiletools.commons.R.string.other
            }
        )
    }
}

fun Context.updateBottomTabItemColors(view: View?, isActive: Boolean, drawableId: Int? = null) {
    val color = if (isActive) {
        getProperPrimaryColor()
    } else {
        getProperTextColor()
    }

    if (drawableId != null) {
        val drawable = ResourcesCompat.getDrawable(resources, drawableId, theme)
        view?.findViewById<ImageView>(com.simplemobiletools.commons.R.id.tab_item_icon)?.setImageDrawable(drawable)
    }

    view?.findViewById<ImageView>(com.simplemobiletools.commons.R.id.tab_item_icon)?.applyColorFilter(color)
    view?.findViewById<TextView>(com.simplemobiletools.commons.R.id.tab_item_label)?.setTextColor(color)
}

fun Context.sendEmailIntent(recipient: String) {
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts(KEY_MAILTO, recipient, null)
        launchActivityIntent(this)
    }
}

fun Context.openNotificationSettings() {
    if (isOreoPlus()) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(intent)
    } else {
        // For Android versions below Oreo, you can't directly open the app's notification settings.
        // You can open the general notification settings instead.
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }
}

fun Context.getTempFile(folderName: String, filename: String): File? {
    val folder = File(cacheDir, folderName)
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, filename)
}

fun Context.openDeviceSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }

    try {
        startActivity(intent)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun Context.openRequestExactAlarmSettings(appId: String) {
    if (isSPlus()) {
        val uri = Uri.fromParts("package", appId, null)
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        intent.data = uri
        startActivity(intent)
    }
}

fun Context.canUseFullScreenIntent(): Boolean {
    return !isUpsideDownCakePlus() || notificationManager.canUseFullScreenIntent()
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun Context.openFullScreenIntentSettings(appId: String) {
    if (isUpsideDownCakePlus()) {
        val uri = Uri.fromParts("package", appId, null)
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        intent.data = uri
        startActivity(intent)
    }
}
