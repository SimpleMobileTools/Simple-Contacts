package com.simplemobiletools.contacts.pro.extensions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.FileProvider
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.hasPermission
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CONTACTS
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CONTACTS
import com.simplemobiletools.contacts.pro.BuildConfig
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.activities.EditContactActivity
import com.simplemobiletools.contacts.pro.activities.ViewContactActivity
import com.simplemobiletools.contacts.pro.databases.ContactsDatabase
import com.simplemobiletools.contacts.pro.helpers.CONTACT_ID
import com.simplemobiletools.contacts.pro.helpers.Config
import com.simplemobiletools.contacts.pro.helpers.IS_PRIVATE
import com.simplemobiletools.contacts.pro.helpers.SMT_PRIVATE
import com.simplemobiletools.contacts.pro.interfaces.ContactsDao
import com.simplemobiletools.contacts.pro.interfaces.GroupsDao
import com.simplemobiletools.contacts.pro.models.Contact
import com.simplemobiletools.contacts.pro.models.Organization
import java.io.File

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.contactsDB: ContactsDao get() = ContactsDatabase.getInstance(applicationContext).ContactsDao()

val Context.groupsDB: GroupsDao get() = ContactsDatabase.getInstance(applicationContext).GroupsDao()

fun Context.getEmptyContact(): Contact {
    val originalContactSource = if (hasContactPermissions()) config.lastUsedContactSource else SMT_PRIVATE
    val organization = Organization("", "")
    return Contact(0, "", "", "", "", "", "", "", ArrayList(), ArrayList(), ArrayList(), ArrayList(), originalContactSource, 0, 0, "",
            null, "", ArrayList(), organization, ArrayList(), ArrayList(), ArrayList())
}

fun Context.viewContact(contact: Contact) {
    Intent(applicationContext, ViewContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.source == SMT_PRIVATE)
        startActivity(this)
    }
}

fun Context.editContact(contact: Contact) {
    Intent(applicationContext, EditContactActivity::class.java).apply {
        putExtra(CONTACT_ID, contact.id)
        putExtra(IS_PRIVATE, contact.source == SMT_PRIVATE)
        startActivity(this)
    }
}

fun Context.sendEmailIntent(recipient: String) {
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("mailto", recipient, null)
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
        if (cursor.moveToFirst()) {
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
