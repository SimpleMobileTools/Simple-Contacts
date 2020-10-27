package com.simplemobiletools.contacts.pro.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.FileProvider
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.EditContactActivity
import com.simplemobiletools.contacts.pro.activities.ViewContactActivity
import com.simplemobiletools.contacts.pro.databases.ContactsDatabase
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.ContactsDao
import com.simplemobiletools.contacts.pro.interfaces.GroupsDao
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.ContactSource
import com.simplemobiletools.contacts.pro.models.Organization
import com.simplemobiletools.contacts.pro.models.SocialAction
import java.io.File

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.contactsDB: ContactsDao get() = ContactsDatabase.getInstance(applicationContext).ContactsDao()

val Context.groupsDB: GroupsDao get() = ContactsDatabase.getInstance(applicationContext).GroupsDao()

fun Context.getEmptyContact(): Contact {
    val originalContactSource = if (hasContactPermissions()) config.lastUsedContactSource else SMT_PRIVATE
    val organization = Organization("", "")
    return Contact(0, "", "", "", "", "", "", "", ArrayList(), ArrayList(), ArrayList(), ArrayList(), originalContactSource, 0, 0, "",
        null, "", ArrayList(), organization, ArrayList(), ArrayList())
}

fun Context.viewContact(contact: Contact) {
    Intent(applicationContext, ViewContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.isPrivate())
        startActivity(this)
    }
}

fun Context.editContact(contact: Contact) {
    Intent(applicationContext, EditContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.isPrivate())
        startActivity(this)
    }
}

fun Context.sendEmailIntent(recipient: String) {
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts(KEY_MAILTO, recipient, null)
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Context.sendSMSIntent(recipient: String) {
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("smsto", recipient, null)
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Context.sendAddressIntent(address: String) {
    val location = Uri.encode(address)
    val uri = Uri.parse("geo:0,0?q=$location")

    Intent(Intent.ACTION_VIEW, uri).apply {
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Context.openWebsiteIntent(url: String) {
    Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Context.getLookupUriRawId(dataUri: Uri): Int {
    val lookupKey = getLookupKeyFromUri(dataUri)
    if (lookupKey != null) {
        val uri = lookupContactUri(lookupKey, this)
        if (uri != null) {
            return getContactUriRawId(uri)
        }
    }
    return -1
}

fun Context.getContactUriRawId(uri: Uri): Int {
    val projection = arrayOf(ContactsContract.Contacts.NAME_RAW_CONTACT_ID)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor!!.moveToFirst()) {
            return cursor.getIntValue(ContactsContract.Contacts.NAME_RAW_CONTACT_ID)
        }
    } catch (ignored: Exception) {
    } finally {
        cursor?.close()
    }
    return -1
}

// from https://android.googlesource.com/platform/packages/apps/Dialer/+/68038172793ee0e2ab3e2e56ddfbeb82879d1f58/java/com/android/contacts/common/util/UriUtils.java
fun getLookupKeyFromUri(lookupUri: Uri): String? {
    return if (!isEncodedContactUri(lookupUri)) {
        val segments = lookupUri.pathSegments
        if (segments.size < 3) null else Uri.encode(segments[2])
    } else {
        null
    }
}

fun isEncodedContactUri(uri: Uri?): Boolean {
    if (uri == null) {
        return false
    }
    val lastPathSegment = uri.lastPathSegment ?: return false
    return lastPathSegment == "encoded"
}

fun lookupContactUri(lookup: String, context: Context): Uri? {
    val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookup)
    return try {
        ContactsContract.Contacts.lookupContact(context.contentResolver, lookupUri)
    } catch (e: Exception) {
        null
    }
}

fun Context.getCachePhoto(): File {
    val imagesFolder = File(cacheDir, "my_cache")
    if (!imagesFolder.exists()) {
        imagesFolder.mkdirs()
    }

    val file = File(imagesFolder, "Photo_${System.currentTimeMillis()}.jpg")
    file.createNewFile()
    return file
}

fun Context.getCachePhotoUri(file: File = getCachePhoto()) = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)

fun Context.getPhotoThumbnailSize(): Int {
    val uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI
    val projection = arrayOf(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getIntValue(ContactsContract.DisplayPhoto.THUMBNAIL_MAX_DIM)
        }
    } catch (ignored: Exception) {
    } finally {
        cursor?.close()
    }
    return 0
}

fun Context.hasContactPermissions() = hasPermission(PERMISSION_READ_CONTACTS) && hasPermission(PERMISSION_WRITE_CONTACTS)

fun Context.getPublicContactSource(source: String, callback: (String) -> Unit) {
    when (source) {
        SMT_PRIVATE -> callback(getString(R.string.phone_storage_hidden))
        else -> {
            ContactsHelper(this).getContactSources {
                var newSource = source
                for (contactSource in it) {
                    if (contactSource.name == source && contactSource.type == TELEGRAM_PACKAGE) {
                        newSource += " (${getString(R.string.telegram)})"
                        break
                    }
                }
                Handler(Looper.getMainLooper()).post {
                    callback(newSource)
                }
            }
        }
    }
}

fun Context.getPublicContactSourceSync(source: String, contactSources: ArrayList<ContactSource>): String {
    return when (source) {
        SMT_PRIVATE -> getString(R.string.phone_storage_hidden)
        else -> {
            var newSource = source
            for (contactSource in contactSources) {
                if (contactSource.name == source && contactSource.type == TELEGRAM_PACKAGE) {
                    newSource += " (${getString(R.string.telegram)})"
                    break
                }
            }

            return newSource
        }
    }
}

fun Context.sendSMSToContacts(contacts: ArrayList<Contact>) {
    val numbers = StringBuilder()
    contacts.forEach {
        val number = it.phoneNumbers.firstOrNull { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE }
            ?: it.phoneNumbers.firstOrNull()
        if (number != null) {
            numbers.append("${Uri.encode(number.value)};")
        }

        val uriString = "smsto:${numbers.toString().trimEnd(';')}"
        Intent(Intent.ACTION_SENDTO, Uri.parse(uriString)).apply {
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun Context.sendEmailToContacts(contacts: ArrayList<Contact>) {
    val emails = ArrayList<String>()
    contacts.forEach {
        it.emails.forEach {
            if (it.value.isNotEmpty()) {
                emails.add(it.value)
            }
        }
    }

    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, emails.toTypedArray())
        if (resolveActivity(packageManager) != null) {
            startActivity(this)
        } else {
            toast(R.string.no_app_found)
        }
    }
}

fun Context.getTempFile(): File? {
    val folder = File(cacheDir, "contacts")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "contacts.vcf")
}

fun Context.addContactsToGroup(contacts: ArrayList<Contact>, groupId: Long) {
    val publicContacts = contacts.filter { !it.isPrivate() }.toMutableList() as ArrayList<Contact>
    val privateContacts = contacts.filter { it.isPrivate() }.toMutableList() as ArrayList<Contact>
    if (publicContacts.isNotEmpty()) {
        ContactsHelper(this).addContactsToGroup(publicContacts, groupId)
    }

    if (privateContacts.isNotEmpty()) {
        LocalContactsHelper(this).addContactsToGroup(privateContacts, groupId)
    }
}

fun Context.removeContactsFromGroup(contacts: ArrayList<Contact>, groupId: Long) {
    val publicContacts = contacts.filter { !it.isPrivate() }.toMutableList() as ArrayList<Contact>
    val privateContacts = contacts.filter { it.isPrivate() }.toMutableList() as ArrayList<Contact>
    if (publicContacts.isNotEmpty() && hasContactPermissions()) {
        ContactsHelper(this).removeContactsFromGroup(publicContacts, groupId)
    }

    if (privateContacts.isNotEmpty()) {
        LocalContactsHelper(this).removeContactsFromGroup(privateContacts, groupId)
    }
}

fun Context.getContactPublicUri(contact: Contact): Uri {
    val lookupKey = if (contact.isPrivate()) {
        "local_${contact.id}"
    } else {
        SimpleContactsHelper(this).getContactLookupKey(contact.id.toString())
    }
    return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
}

fun Context.getVisibleContactSources(): ArrayList<String> {
    val sources = getAllContactSources()
    val ignoredContactSources = config.ignoredContactSources
    return ArrayList(sources).filter { !ignoredContactSources.contains(it.getFullIdentifier()) }
        .map { it.name }.toMutableList() as ArrayList<String>
}

fun Context.getAllContactSources(): ArrayList<ContactSource> {
    val sources = ContactsHelper(this).getDeviceContactSources()
    sources.add(getPrivateContactSource())
    return sources.toMutableList() as ArrayList<ContactSource>
}

fun Context.getPrivateContactSource() = ContactSource(SMT_PRIVATE, SMT_PRIVATE, getString(R.string.phone_storage_hidden))

fun Context.getWhatsAppActions(id: Int): ArrayList<SocialAction> {
    val uri = ContactsContract.Data.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.Data._ID,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.ACCOUNT_TYPE_AND_DATA_SET
    )

    val socialActions = ArrayList<SocialAction>()
    var curActionId = 0
    val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    queryCursor(uri, projection, selection, selectionArgs, null, true) { cursor ->
        val mimetype = cursor.getStringValue(ContactsContract.Data.MIMETYPE)
        val type = when (mimetype) {
            "vnd.android.cursor.item/vnd.com.whatsapp.profile" -> SOCIAL_MESSAGE
            "vnd.android.cursor.item/vnd.com.whatsapp.voip.call" -> SOCIAL_VOICE_CALL
            "vnd.android.cursor.item/vnd.com.whatsapp.video.call" -> SOCIAL_VIDEO_CALL
            else -> return@queryCursor
        }

        val label = cursor.getStringValue(ContactsContract.Data.DATA3)
        val realID = cursor.getLongValue(ContactsContract.Data._ID)
        val packageName = cursor.getStringValue(ContactsContract.Data.ACCOUNT_TYPE_AND_DATA_SET)
        val socialAction = SocialAction(curActionId++, type, label, mimetype, realID, packageName)
        socialActions.add(socialAction)
    }
    return socialActions
}

fun Context.getSignalActions(id: Int): ArrayList<SocialAction> {
    val uri = ContactsContract.Data.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.Data._ID,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.ACCOUNT_TYPE_AND_DATA_SET
    )

    val socialActions = ArrayList<SocialAction>()
    var curActionId = 0
    val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ?"
    val selectionArgs = arrayOf(id.toString())
    queryCursor(uri, projection, selection, selectionArgs, null, true) { cursor ->
        val mimetype = cursor.getStringValue(ContactsContract.Data.MIMETYPE)
        val type = when (mimetype) {
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.contact" -> SOCIAL_MESSAGE
            "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call" -> SOCIAL_VOICE_CALL
            else -> return@queryCursor
        }

        val label = cursor.getStringValue(ContactsContract.Data.DATA3)
        val realID = cursor.getLongValue(ContactsContract.Data._ID)
        val packageName = cursor.getStringValue(ContactsContract.Data.ACCOUNT_TYPE_AND_DATA_SET)
        val socialAction = SocialAction(curActionId++, type, label, mimetype, realID, packageName)
        socialActions.add(socialAction)
    }
    return socialActions
}

fun Context.getPackageDrawable(packageName: String): Drawable? {
    var drawable: Drawable? = null
    try {
        // try getting the properly colored launcher icons
        val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityList = launcher.getActivityList(packageName, android.os.Process.myUserHandle())[0]
        drawable = activityList.getBadgedIcon(0)
    } catch (ignored: Exception) {
    }

    if (drawable == null) {
        try {
            drawable = packageManager.getApplicationIcon(packageName)
        } catch (ignored: Exception) {
        }
    }

    return drawable
}
