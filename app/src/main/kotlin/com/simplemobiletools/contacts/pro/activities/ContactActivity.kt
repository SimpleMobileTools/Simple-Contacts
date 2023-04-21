package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ContactsHelper
import com.simplemobiletools.commons.helpers.letterBackgroundColors
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.extensions.shareContacts
import com.simplemobiletools.commons.models.contacts.Contact
import com.simplemobiletools.commons.models.contacts.ContactRelation
import com.simplemobiletools.commons.models.contacts.IM

data class ContactActivityState(
    var stateValid: Boolean = false,

    var autoCalcDisplayName: Boolean = false,
    var autoCalcFormattedAddress: ArrayList<Boolean> = ArrayList<Boolean>(),

    var selectShareFieldsActive: Boolean = false,
    var sharePhoto: Boolean = true,
    var shareName: Boolean = true,
    var shareNickname: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareNumber: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareEmail: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareAddress: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareIM: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareEvent: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareNotes: Boolean = true,
    // var shareRingtone: Boolean = false,
    var shareOrganization: Boolean = true,
    var shareWebsite: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareRelation: ArrayList<Boolean> = ArrayList<Boolean>(),
    var shareGroup: ArrayList<Boolean> = ArrayList<Boolean>(),
    // var shareSources: Boolean = false,

    var vScrollWidth: Int = 1000,
    var vScrollHeight: Int = 1000,
    var vScrollPosX: Int = 1,
    var vScrollPosY: Int = 1
) {
    fun reset() {
        stateValid = false

        autoCalcDisplayName = false
        autoCalcFormattedAddress.clear()

        selectShareFieldsActive = false
        sharePhoto = true
        shareName = true
        shareNickname.clear()
        shareNumber.clear()
        shareEmail.clear()
        shareAddress.clear()
        shareIM.clear()
        shareEvent.clear()
        shareNotes = true
        shareOrganization = true
        shareWebsite.clear()
        shareRelation.clear()
        shareGroup.clear()
        // shareRingtone = false,
        // shareSources = false,

        vScrollWidth = 1000
        vScrollHeight = 1000
        vScrollPosX = 1
        vScrollPosY = 1
    } // ContactActivityState.reset()

    fun initShareInfo(srcContact: Contact) {
        var itemCnt: Int
        selectShareFieldsActive = false
        sharePhoto = true
        shareName = true

        shareNickname.clear()
        itemCnt = srcContact.nicknames.count()
        for (i in 1..itemCnt)
            shareNickname.add(true)

        shareNumber.clear()
        itemCnt = srcContact.phoneNumbers.count()
        for (i in 1..itemCnt)
            shareNumber.add(true)

        shareEmail.clear()
        itemCnt = srcContact.emails.count()
        for (i in 1..itemCnt)
            shareEmail.add(true)

        shareAddress.clear()
        itemCnt = srcContact.addresses.count()
        for (i in 1..itemCnt)
            shareAddress.add(true)

        shareIM.clear()
        itemCnt = srcContact.IMs.count()
        for (i in 1..itemCnt)
            shareIM.add(true)

        shareEvent.clear()
        itemCnt = srcContact.events.count()
        for (i in 1..itemCnt)
            shareEvent.add(true)

        shareNotes = true
        shareOrganization = true

        shareWebsite.clear()
        itemCnt = srcContact.websites.count()
        for (i in 1..itemCnt)
            shareWebsite.add(true)

        shareRelation.clear()
        itemCnt = srcContact.relations.count()
        for (i in 1..itemCnt)
            shareRelation.add(true)

        shareGroup.clear()
        itemCnt = srcContact.groups.count()
        for (i in 1..itemCnt)
            shareGroup.add(true)

        // shareRingtone = false
        // shareSources = false
    } // ContactActivityState.initShareInfo()
} // class ContactActivityState

abstract class ContactActivity : SimpleActivity() {
    protected val PICK_RINGTONE_INTENT_ID = 1500
    protected val INTENT_SELECT_RINGTONE = 600

    private val gson = Gson()
    val activityStateType = object : TypeToken<ContactActivityState>() {}.type
    val contactType = object : TypeToken<Contact>() {}.type

    protected var contact: Contact? = null
    protected var origContact: Contact? = null
    protected var state: ContactActivityState = ContactActivityState()

    protected var originalRingtone: String? = null
    protected var currentContactPhotoPath = ""
    protected var useFamilyNameForPlaceholderIcon: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contact = restoreContact(savedInstanceState, "ActiveContact")
        origContact = restoreContact(savedInstanceState, "OrigContact")

        var restoredState: ContactActivityState? = null
        val jsonState: String? = savedInstanceState?.getString("ActivityState")
        if (jsonState != null) {
            // restoredState = Json.decodeFromString<ContactActivityState>(jsonState)
            restoredState = gson.fromJson<ContactActivityState>(jsonState, activityStateType)
        }
        state = (if (restoredState != null) restoredState else ContactActivityState())
    } // ContactActivity.onCreate()

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        saveContact(savedInstanceState, "ActiveContact", contact)
        saveContact(savedInstanceState, "OrigContact", origContact)

        // val jsonState: String = Json.encodeToString<ContactActivityState>(state)
        val jsonState: String = gson.toJson(state)
        savedInstanceState.putString("ActivityState", jsonState)

        super.onSaveInstanceState(savedInstanceState)
    } // ContactActivity.onSaveInstanceState()

    fun saveContact(savedInstanceState: Bundle?, label: String, srcContact: Contact?) {
        if (srcContact != null) {
            try {
                // val jsonState: String = Json.encodeToString<Contact>(srcContact)
                val jsonState: String = gson.toJson(srcContact)
                savedInstanceState?.putString(label, jsonState)
            }
            catch (e: Exception) {
                Log.e("SaveContact($label)", e.toString())
            }
        } else
            savedInstanceState?.remove(label)
    } // ContactActivity.saveContact()

    fun restoreContact(savedInstanceState: Bundle?, label: String): Contact? {
        val jsonState: String? = savedInstanceState?.getString(label)
        if (jsonState != null) {
            // return (Json.decodeFromString<Contact>(jsonState))
            return (gson.fromJson<Contact>(jsonState, contactType))
        } else {
            return (null)
        }
    } // ContactActivity.restoreContact()

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_RINGTONE_INTENT_ID && resultCode == RESULT_OK && resultData != null && resultData.dataString != null) {
            customRingtoneSelected(Uri.decode(resultData.dataString!!))
        } else if (requestCode == INTENT_SELECT_RINGTONE && resultCode == Activity.RESULT_OK && resultData != null) {
            val extras = resultData.extras
            if (extras?.containsKey(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) == true) {
                val uri = extras.getParcelable<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                try {
                    systemRingtoneSelected(uri)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    abstract fun customRingtoneSelected(ringtonePath: String)

    abstract fun systemRingtoneSelected(uri: Uri?)

    fun showPhotoPlaceholder(photoView: ImageView) {
        val placeholder = BitmapDrawable(resources, getBigLetterPlaceholder(contact?.getNameToDisplay() ?: "A"))
        photoView.setImageDrawable(placeholder)
        currentContactPhotoPath = ""
        contact?.photo = null
    }

    fun updateContactPhoto(path: String, photoView: ImageView, bottomShadow: ImageView, bitmap: Bitmap? = null) {
        currentContactPhotoPath = path

        if (isDestroyed || isFinishing) {
            return
        }

        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()

        val wantedWidth = realScreenSize.x
        val wantedHeight = resources.getDimension(R.dimen.top_contact_image_height).toInt()

        Glide.with(this)
            .load(bitmap ?: path)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .override(wantedWidth, wantedHeight)
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    photoView.background = ColorDrawable(0)
                    bottomShadow.beVisible()
                    return false
                }

                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    showPhotoPlaceholder(photoView)
                    bottomShadow.beGone()
                    return true
                }
            }).into(photoView)
    }

    fun deleteContact() {
        ConfirmationDialog(this) {
            if (contact != null) {
                ContactsHelper(this).deleteContact(contact!!, false) {
                    finish()
                }
            }
        }
    }

    fun shareContact(contact: Contact) {
        shareContacts(arrayListOf(contact))
    }

    fun trySendSMS() {
        val numbers = contact!!.phoneNumbers
        if (numbers.size == 1) {
            launchSendSMSIntent(numbers.first().value)
        } else if (numbers.size > 1) {
            val primaryNumber = numbers.find { it.isPrimary }
            if (primaryNumber != null) {
                launchSendSMSIntent(primaryNumber.value)
            } else {
                val items = ArrayList<RadioItem>()
                numbers.forEachIndexed { index, phoneNumber ->
                    items.add(RadioItem(index, phoneNumber.value, phoneNumber.value))
                }

                RadioGroupDialog(this, items) {
                    launchSendSMSIntent(it as String)
                }
            }
        }
    }

    fun trySendEmail() {
        val emails = contact!!.emails
        if (emails.size == 1) {
            sendEmailIntent(emails.first().address)
        } else if (emails.size > 1) {
            val items = ArrayList<RadioItem>()
            emails.forEachIndexed { index, email ->
                items.add(RadioItem(index, email.address, email.address))
            }

            RadioGroupDialog(this, items) {
                sendEmailIntent(it as String)
            }
        }
    }

    fun getEmailTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Email.TYPE_HOME -> R.string.home
                    Email.TYPE_WORK -> R.string.work
                    Email.TYPE_MOBILE -> R.string.mobile
                    else -> R.string.other
                }
            )
        }
    } // ContactActivity.getEmailTypeText()

    fun getAddressTypeText(type: Int, label: String): String {
        return if (type == BaseTypes.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    StructuredPostal.TYPE_HOME -> R.string.home
                    StructuredPostal.TYPE_WORK -> R.string.work
                    // StructuredPostal.TYPE_OTHER -> R.string.other
                    else -> R.string.other
                }
            )
        }
    } // ContactActivity.getAddressTypeText()

    fun getIMTypeText(type: Int, label: String): String {
        return if (type == Im.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Im.TYPE_HOME -> R.string.home
                    Im.TYPE_WORK -> R.string.work
                    // Im.TYPE_OTHER -> R.string.other
                    else -> R.string.other
                }
            )
        }
    } // ContactActivity.getIMTypeText()

    fun getIMProtocolText(protocol: Int, custom_protocol: String): String {
        return if (protocol == Im.PROTOCOL_CUSTOM) {
            custom_protocol
        } else {
            getString(
                when (protocol) {
                    Im.PROTOCOL_AIM -> R.string.instantmsg_aim
                    Im.PROTOCOL_MSN -> R.string.instantmsg_windows_live
                    Im.PROTOCOL_YAHOO -> R.string.instantmsg_yahoo
                    Im.PROTOCOL_SKYPE -> R.string.instantmsg_skype
                    Im.PROTOCOL_QQ -> R.string.instantmsg_qq
                    Im.PROTOCOL_GOOGLE_TALK -> R.string.instantmsg_hangouts
                    Im.PROTOCOL_ICQ -> R.string.instantmsg_icq
                    Im.PROTOCOL_JABBER -> R.string.instantmsg_jabber
                    Im.PROTOCOL_NETMEETING -> R.string.instantmsg_netmeeting

                    IM.PROTOCOL_SIP -> R.string.instantmsg_sip
                    IM.PROTOCOL_IRC -> R.string.instantmsg_irc

                    IM.PROTOCOL_MATRIX -> R.string.instantmsg_matrix
                    IM.PROTOCOL_MASTODON -> R.string.instantmsg_mastodon
                    IM.PROTOCOL_SIGNAL -> R.string.instantmsg_signal
                    IM.PROTOCOL_TELEGRAM -> R.string.instantmsg_telegram
                    IM.PROTOCOL_DIASPORA -> R.string.instantmsg_diaspora
                    IM.PROTOCOL_VIBER -> R.string.instantmsg_viber
                    IM.PROTOCOL_THREEMA -> R.string.instantmsg_threema
                    IM.PROTOCOL_DISCORD -> R.string.instantmsg_discord
                    IM.PROTOCOL_MUMBLE -> R.string.instantmsg_mumble
                    IM.PROTOCOL_OLVID -> R.string.instantmsg_olvid
                    IM.PROTOCOL_TEAMSPEAK -> R.string.instantmsg_teamspeak
                    IM.PROTOCOL_FACEBOOK -> R.string.instantmsg_facebook
                    IM.PROTOCOL_INSTAGRAM -> R.string.instantmsg_instagram
                    IM.PROTOCOL_WHATSAPP -> R.string.instantmsg_whatsapp
                    IM.PROTOCOL_TWITTER -> R.string.instantmsg_twitter
                    IM.PROTOCOL_WECHAT -> R.string.instantmsg_wechat
                    IM.PROTOCOL_WEIBO -> R.string.instantmsg_weibo
                    IM.PROTOCOL_TIKTOK -> R.string.instantmsg_tiktok
                    IM.PROTOCOL_TUMBLR -> R.string.instantmsg_tumblr
                    IM.PROTOCOL_FLICKR -> R.string.instantmsg_flickr
                    IM.PROTOCOL_LINKEDIN -> R.string.instantmsg_linkedin
                    IM.PROTOCOL_XING -> R.string.instantmsg_xing
                    IM.PROTOCOL_KIK -> R.string.instantmsg_kik
                    IM.PROTOCOL_LINE -> R.string.instantmsg_line
                    IM.PROTOCOL_KAKAOTALK -> R.string.instantmsg_kakaotalk
                    IM.PROTOCOL_ZOOM -> R.string.instantmsg_zoom
                    IM.PROTOCOL_GITHUB -> R.string.instantmsg_github
                    IM.PROTOCOL_GOOGLEPLUS -> R.string.instantmsg_googleplus
                    IM.PROTOCOL_PINTEREST -> R.string.instantmsg_pinterest
                    // IM.PROTOCOL_QZONE -> R.string.instantmsg_qzone
                    IM.PROTOCOL_YOUTUBE -> R.string.instantmsg_youtube
                    IM.PROTOCOL_SNAPCHAT -> R.string.instantmsg_snapchat
                    IM.PROTOCOL_TEAMS -> R.string.instantmsg_teams
                    IM.PROTOCOL_GOOGLEMEET -> R.string.instantmsg_googlemeet
                    IM.PROTOCOL_TEAMVIEWERMEET -> R.string.instantmsg_teamviewermeet
                    IM.PROTOCOL_NEXTCLOUDTALK -> R.string.instantmsg_nextcloudtalk
                    IM.PROTOCOL_SLACK -> R.string.instantmsg_slack
                    IM.PROTOCOL_JITSI -> R.string.instantmsg_jitsi
                    IM.PROTOCOL_WEBEX -> R.string.instantmsg_webex
                    IM.PROTOCOL_GOTOMEETING -> R.string.instantmsg_gotomeeting
                    IM.PROTOCOL_BIGBLUEBUTTON -> R.string.instantmsg_bigbluebutton
                    else -> R.string.instantmsg_jabber
                }
            )
        }
    } // ContactActivity.getIMProtocolText()

    fun getIMProtocolTypeFromText(value: String): Int = when (value.lowercase()) {
        getString(R.string.instantmsg_aim).lowercase() -> Im.PROTOCOL_AIM
        getString(R.string.instantmsg_aim_uri_scheme)  -> IM.PROTOCOL_AIM
        getString(R.string.instantmsg_windows_live).lowercase() -> Im.PROTOCOL_MSN
        getString(R.string.instantmsg_windows_live_uri_scheme)  -> IM.PROTOCOL_MSN
        getString(R.string.instantmsg_yahoo).lowercase() -> Im.PROTOCOL_YAHOO
        getString(R.string.instantmsg_yahoo_uri_scheme)  -> IM.PROTOCOL_YAHOO
        getString(R.string.instantmsg_skype).lowercase() -> Im.PROTOCOL_SKYPE
        getString(R.string.instantmsg_skype_uri_scheme)  -> IM.PROTOCOL_SKYPE
        getString(R.string.instantmsg_qq).lowercase() -> Im.PROTOCOL_QQ
        getString(R.string.instantmsg_hangouts).lowercase() -> Im.PROTOCOL_GOOGLE_TALK
        getString(R.string.instantmsg_icq).lowercase() -> Im.PROTOCOL_ICQ
        getString(R.string.instantmsg_icq_uri_scheme) -> Im.PROTOCOL_ICQ
        getString(R.string.instantmsg_jabber).lowercase() -> Im.PROTOCOL_JABBER
        getString(R.string.instantmsg_jabber_uri_scheme) -> Im.PROTOCOL_JABBER
        getString(R.string.instantmsg_netmeeting).lowercase() -> Im.PROTOCOL_NETMEETING

        getString(R.string.instantmsg_sip).lowercase()    -> IM.PROTOCOL_SIP
        getString(R.string.instantmsg_sip_uri_scheme)  -> IM.PROTOCOL_SIP
        getString(R.string.instantmsg_sips_uri_scheme)  -> IM.PROTOCOL_SIP
        getString(R.string.instantmsg_irc).lowercase()    -> IM.PROTOCOL_IRC
        getString(R.string.instantmsg_irc_uri_scheme)  -> IM.PROTOCOL_IRC
        getString(R.string.instantmsg_ircs_uri_scheme) -> IM.PROTOCOL_IRC
        getString(R.string.instantmsg_xmpp).lowercase()   -> Im.PROTOCOL_JABBER
        getString(R.string.instantmsg_xmpp_uri_scheme) -> Im.PROTOCOL_JABBER
        getString(R.string.instantmsg_matrix).lowercase() -> IM.PROTOCOL_MATRIX
        getString(R.string.instantmsg_matrix_alt).lowercase() -> IM.PROTOCOL_MATRIX
        getString(R.string.instantmsg_matrix_uri_scheme) -> IM.PROTOCOL_MATRIX
        getString(R.string.instantmsg_mastodon).lowercase() -> IM.PROTOCOL_MASTODON
        getString(R.string.instantmsg_mastodon_uri_scheme).lowercase() -> IM.PROTOCOL_MASTODON
        getString(R.string.instantmsg_signal).lowercase() -> IM.PROTOCOL_SIGNAL
        getString(R.string.instantmsg_signal_uri_scheme).lowercase() -> IM.PROTOCOL_SIGNAL
        getString(R.string.instantmsg_telegram).lowercase() -> IM.PROTOCOL_TELEGRAM
        getString(R.string.instantmsg_telegram_uri_scheme).lowercase() -> IM.PROTOCOL_TELEGRAM
        getString(R.string.instantmsg_diaspora).lowercase() -> IM.PROTOCOL_DIASPORA
        getString(R.string.instantmsg_diaspora_uri_scheme).lowercase() -> IM.PROTOCOL_DIASPORA

        getString(R.string.instantmsg_viber).lowercase()    -> IM.PROTOCOL_VIBER
        getString(R.string.instantmsg_threema).lowercase()  -> IM.PROTOCOL_THREEMA
        getString(R.string.instantmsg_discord).lowercase()  -> IM.PROTOCOL_DISCORD
        getString(R.string.instantmsg_mumble).lowercase()   -> IM.PROTOCOL_MUMBLE
        getString(R.string.instantmsg_olvid).lowercase()    -> IM.PROTOCOL_OLVID
        getString(R.string.instantmsg_teamspeak).lowercase()-> IM.PROTOCOL_TEAMSPEAK

        getString(R.string.instantmsg_facebook).lowercase() -> IM.PROTOCOL_FACEBOOK
        getString(R.string.instantmsg_instagram).lowercase()-> IM.PROTOCOL_INSTAGRAM
        getString(R.string.instantmsg_whatsapp).lowercase() -> IM.PROTOCOL_WHATSAPP
        getString(R.string.instantmsg_twitter).lowercase()  -> IM.PROTOCOL_TWITTER
        getString(R.string.instantmsg_wechat).lowercase()   -> IM.PROTOCOL_WECHAT
        getString(R.string.instantmsg_weibo).lowercase()    -> IM.PROTOCOL_WEIBO
        getString(R.string.instantmsg_tiktok).lowercase()   -> IM.PROTOCOL_TIKTOK
        getString(R.string.instantmsg_tumblr).lowercase()   -> IM.PROTOCOL_TUMBLR
        getString(R.string.instantmsg_flickr).lowercase()   -> IM.PROTOCOL_FLICKR
        getString(R.string.instantmsg_linkedin).lowercase() -> IM.PROTOCOL_LINKEDIN
        getString(R.string.instantmsg_xing).lowercase()     -> IM.PROTOCOL_XING
        else -> Im.PROTOCOL_CUSTOM
    } // ContactActivity.getIMProtocolTypeFromText()

    fun getIMProtocolURIScheme(protocol: Int, custom_protocol: String): String {
        return when (protocol) {
            Im.PROTOCOL_AIM    -> getString(R.string.instantmsg_aim_uri_scheme)
            Im.PROTOCOL_MSN    -> getString(R.string.instantmsg_windows_live_uri_scheme)
            Im.PROTOCOL_YAHOO  -> getString(R.string.instantmsg_yahoo_uri_scheme)
            Im.PROTOCOL_SKYPE  -> getString(R.string.instantmsg_skype_uri_scheme)
            Im.PROTOCOL_ICQ    -> getString(R.string.instantmsg_icq_uri_scheme)
            IM.PROTOCOL_SIP    -> getString(R.string.instantmsg_sips_uri_scheme)
            IM.PROTOCOL_IRC    -> getString(R.string.instantmsg_ircs_uri_scheme)
            IM.PROTOCOL_MATRIX -> getString(R.string.instantmsg_matrix_uri_scheme)
            // IM.PROTOCOL_JABBER -> getString(R.string.instantmsg_jabber_uri_scheme)
            IM.PROTOCOL_JABBER -> getString(R.string.instantmsg_xmpp_uri_scheme)
            else -> getIMProtocolText(protocol, custom_protocol)
        }
    } // ContactActivity:getIMProtocolURIScheme()

    fun convertKnownIMCustomTypesToIDs(IMs: ArrayList<IM>) {
        IMs.forEach { convertIM ->
            if (convertIM.protocol == Im.PROTOCOL_CUSTOM) {
                val custom_protocol = convertIM.custom_protocol.trim()
                val protocol = getIMProtocolTypeFromText(custom_protocol)
                if (protocol != Im.PROTOCOL_CUSTOM) {
                    convertIM.protocol = protocol
                    convertIM.custom_protocol = ""
                }
            }
        }
    } // ContactActivity.convertKnownIMCustomTypesToIDs()

    fun convertKnownIMTypeIDsToCustomTypes(IMs: ArrayList<IM>) {
        val androidVersion: Int = android.os.Build.VERSION.SDK_INT
        // All Instant Msg. Protocols other than PROTOCOL_CUSTOM
        // were deprecated in Android API Version 31 (Android 12 - "Snow Cone")
        // If we are running on a device using API 31+, we shall
        // convert all protocol information to PROTOCOL_CUSTOM
        // even if there was a dedicated PROTOCOL_xxx constant
        // in previous versions.
        val convertAllToCustom: Boolean = (androidVersion >= 31)
        IMs.forEach { convertIM ->
            if (convertIM.protocol != Im.PROTOCOL_CUSTOM) {
                if (convertAllToCustom || (convertIM.protocol > Im.PROTOCOL_NETMEETING)) {
                    val protocol = getIMProtocolTypeFromText(convertIM.custom_protocol.trim())
                    if (convertIM.protocol != protocol)
                        convertIM.custom_protocol = getIMProtocolURIScheme(convertIM.protocol, "")
                    convertIM.protocol = Im.PROTOCOL_CUSTOM
                } else {
                    convertIM.custom_protocol = ""
                }
            }
        }
    } //  EditContactActivity.convertKnownIMTypeIDsToCustomTypes()

    fun getEventTypeText(type: Int, label: String) : String {
        return if (type == Event.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Event.TYPE_ANNIVERSARY -> R.string.anniversary
                    Event.TYPE_BIRTHDAY -> R.string.birthday
                    // Event.TYPE_OTHER -> R.string.other
                    else -> R.string.other
                }
            )
        }
    } // ContactActivity.getEventTypeText()

    fun getWebsiteTypeText(type: Int, label: String) : String {
        return if (type == Website.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    Website.TYPE_HOMEPAGE -> R.string.website_homepage
                    Website.TYPE_BLOG -> R.string.website_blog
                    Website.TYPE_PROFILE -> R.string.website_profile
                    Website.TYPE_HOME -> R.string.home
                    Website.TYPE_WORK -> R.string.work
                    Website.TYPE_FTP -> R.string.website_ftp
                    // Website.TYPE_OTHER -> R.string.other
                    else -> R.string.other
                }
            )
        }
    } // ContactActivity.getWebsiteTypeText()

    fun getRelationTypeText(type: Int, label: String) : String {
        return if (type == Relation.TYPE_CUSTOM) {
            label
        } else {
            getString(
                when (type) {
                    // Relation.TYPE_CUSTOM -> R.string.relation_CUSTOM
                    Relation.TYPE_ASSISTANT -> R.string.relation_assistant
                    Relation.TYPE_BROTHER -> R.string.relation_brother
                    Relation.TYPE_CHILD -> R.string.relation_child
                    Relation.TYPE_DOMESTIC_PARTNER -> R.string.relation_domestic_partner
                    Relation.TYPE_FATHER -> R.string.relation_father
                    Relation.TYPE_FRIEND -> R.string.relation_friend
                    Relation.TYPE_MANAGER -> R.string.relation_manager
                    Relation.TYPE_MOTHER -> R.string.relation_mother
                    Relation.TYPE_PARENT -> R.string.relation_parent
                    Relation.TYPE_PARTNER -> R.string.relation_partner
                    Relation.TYPE_REFERRED_BY -> R.string.relation_referred_by
                    Relation.TYPE_RELATIVE -> R.string.relation_relative
                    Relation.TYPE_SISTER -> R.string.relation_sister
                    Relation.TYPE_SPOUSE -> R.string.relation_spouse

                    // Relation types defined in vCard 4.0
                    ContactRelation.TYPE_CONTACT -> R.string.relation_contact
                    ContactRelation.TYPE_ACQUAINTANCE -> R.string.relation_acquaintance
                    // ContactRelation.TYPE_FRIEND -> R.string.relation_friend
                    ContactRelation.TYPE_MET -> R.string.relation_met
                    ContactRelation.TYPE_CO_WORKER -> R.string.relation_co_worker
                    ContactRelation.TYPE_COLLEAGUE -> R.string.relation_colleague
                    ContactRelation.TYPE_CO_RESIDENT -> R.string.relation_co_resident
                    ContactRelation.TYPE_NEIGHBOR -> R.string.relation_neighbor
                    // ContactRelation.TYPE_CHILD -> R.string.relation_child
                    // ContactRelation.TYPE_PARENT -> R.string.relation_parent
                    ContactRelation.TYPE_SIBLING -> R.string.relation_sibling
                    // ContactRelation.TYPE_SPOUSE -> R.string.relation_spouse
                    ContactRelation.TYPE_KIN -> R.string.relation_kin
                    ContactRelation.TYPE_MUSE -> R.string.relation_muse
                    ContactRelation.TYPE_CRUSH -> R.string.relation_crush
                    ContactRelation.TYPE_DATE -> R.string.relation_date
                    ContactRelation.TYPE_SWEETHEART -> R.string.relation_sweetheart
                    ContactRelation.TYPE_ME -> R.string.relation_me
                    ContactRelation.TYPE_AGENT -> R.string.relation_agent
                    ContactRelation.TYPE_EMERGENCY -> R.string.relation_emergency

                    // Additional custom types
                    ContactRelation.TYPE_SUPERIOR -> R.string.relation_superior
                    ContactRelation.TYPE_SUBORDINATE -> R.string.relation_subordinate
                    ContactRelation.TYPE_HUSBAND -> R.string.relation_husband
                    ContactRelation.TYPE_WIFE -> R.string.relation_wife
                    ContactRelation.TYPE_SON -> R.string.relation_son
                    ContactRelation.TYPE_DAUGHTER -> R.string.relation_daughter
                    ContactRelation.TYPE_GRANDPARENT -> R.string.relation_grandparent
                    ContactRelation.TYPE_GRANDFATHER -> R.string.relation_grandfather
                    ContactRelation.TYPE_GRANDMOTHER -> R.string.relation_grandmother
                    ContactRelation.TYPE_GRANDCHILD -> R.string.relation_grandchild
                    ContactRelation.TYPE_GRANDSON -> R.string.relation_grandson
                    ContactRelation.TYPE_GRANDDAUGHTER -> R.string.relation_granddaughter
                    ContactRelation.TYPE_UNCLE -> R.string.relation_uncle
                    ContactRelation.TYPE_AUNT -> R.string.relation_aunt
                    ContactRelation.TYPE_NEPHEW -> R.string.relation_nephew
                    ContactRelation.TYPE_NIECE -> R.string.relation_niece
                    ContactRelation.TYPE_FATHER_IN_LAW -> R.string.relation_father_in_law
                    ContactRelation.TYPE_MOTHER_IN_LAW -> R.string.relation_mother_in_law
                    ContactRelation.TYPE_SON_IN_LAW -> R.string.relation_son_in_law
                    ContactRelation.TYPE_DAUGHTER_IN_LAW -> R.string.relation_daughter_in_law
                    ContactRelation.TYPE_BROTHER_IN_LAW -> R.string.relation_brother_in_law
                    ContactRelation.TYPE_SISTER_IN_LAW -> R.string.relation_sister_in_law
                    // Relation.TYPE_OTHER -> R.string.other
                    else -> R.string.other
                } // when (type)
            )  // getString()
        } // else
    } // ContactActivity.getRelationTypeText()

    fun getRelationTypeFromText(text: String): Int {
        return( when(text.lowercase()) {
            getString(R.string.relation_assistant).lowercase() -> ContactRelation.TYPE_ASSISTANT
            getString(R.string.relation_brother).lowercase() -> ContactRelation.TYPE_BROTHER
            getString(R.string.relation_child).lowercase() -> ContactRelation.TYPE_CHILD
            getString(R.string.relation_domestic_partner).lowercase() -> ContactRelation.TYPE_DOMESTIC_PARTNER
            getString(R.string.relation_father).lowercase() -> ContactRelation.TYPE_FATHER
            getString(R.string.relation_friend).lowercase() -> ContactRelation.TYPE_FRIEND
            getString(R.string.relation_manager).lowercase() -> ContactRelation.TYPE_MANAGER
            getString(R.string.relation_mother).lowercase() -> ContactRelation.TYPE_MOTHER
            getString(R.string.relation_parent).lowercase() -> ContactRelation.TYPE_PARENT
            getString(R.string.relation_partner).lowercase() -> ContactRelation.TYPE_PARTNER
            getString(R.string.relation_referred_by).lowercase() -> ContactRelation.TYPE_REFERRED_BY
            getString(R.string.relation_relative).lowercase() -> ContactRelation.TYPE_RELATIVE
            getString(R.string.relation_sister).lowercase() -> ContactRelation.TYPE_SISTER
            getString(R.string.relation_spouse).lowercase() -> ContactRelation.TYPE_SPOUSE

            getString(R.string.relation_contact).lowercase() -> ContactRelation.TYPE_CONTACT
            getString(R.string.relation_acquaintance).lowercase() -> ContactRelation.TYPE_ACQUAINTANCE
            getString(R.string.relation_met).lowercase() -> ContactRelation.TYPE_MET
            getString(R.string.relation_co_worker).lowercase() -> ContactRelation.TYPE_CO_WORKER
            getString(R.string.relation_colleague).lowercase() -> ContactRelation.TYPE_COLLEAGUE
            getString(R.string.relation_co_resident).lowercase() -> ContactRelation.TYPE_CO_RESIDENT
            getString(R.string.relation_neighbor).lowercase() -> ContactRelation.TYPE_NEIGHBOR
            getString(R.string.relation_sibling).lowercase() -> ContactRelation.TYPE_SIBLING
            getString(R.string.relation_kin).lowercase() -> ContactRelation.TYPE_KIN
            getString(R.string.relation_muse).lowercase() -> ContactRelation.TYPE_MUSE
            getString(R.string.relation_crush).lowercase() -> ContactRelation.TYPE_CRUSH
            getString(R.string.relation_date).lowercase() -> ContactRelation.TYPE_DATE
            getString(R.string.relation_sweetheart).lowercase() -> ContactRelation.TYPE_SWEETHEART
            getString(R.string.relation_agent).lowercase() -> ContactRelation.TYPE_AGENT
            getString(R.string.relation_emergency).lowercase() -> ContactRelation.TYPE_EMERGENCY
            getString(R.string.relation_me).lowercase() -> ContactRelation.TYPE_ME

            getString(R.string.relation_superior).lowercase() -> ContactRelation.TYPE_SUPERIOR
            getString(R.string.relation_subordinate).lowercase() -> ContactRelation.TYPE_SUBORDINATE
            getString(R.string.relation_husband).lowercase() -> ContactRelation.TYPE_HUSBAND
            getString(R.string.relation_wife).lowercase() -> ContactRelation.TYPE_WIFE
            getString(R.string.relation_son).lowercase() -> ContactRelation.TYPE_SON
            getString(R.string.relation_daughter).lowercase() -> ContactRelation.TYPE_DAUGHTER
            getString(R.string.relation_grandparent).lowercase() -> ContactRelation.TYPE_GRANDPARENT
            getString(R.string.relation_grandfather).lowercase() -> ContactRelation.TYPE_GRANDFATHER
            getString(R.string.relation_grandmother).lowercase() -> ContactRelation.TYPE_GRANDMOTHER
            getString(R.string.relation_grandchild).lowercase() -> ContactRelation.TYPE_GRANDCHILD
            getString(R.string.relation_grandson).lowercase() -> ContactRelation.TYPE_GRANDSON
            getString(R.string.relation_granddaughter).lowercase() -> ContactRelation.TYPE_GRANDDAUGHTER
            getString(R.string.relation_uncle).lowercase() -> ContactRelation.TYPE_UNCLE
            getString(R.string.relation_aunt).lowercase() -> ContactRelation.TYPE_AUNT
            getString(R.string.relation_nephew).lowercase() -> ContactRelation.TYPE_NEPHEW
            getString(R.string.relation_niece).lowercase() -> ContactRelation.TYPE_NIECE
            getString(R.string.relation_father_in_law).lowercase() -> ContactRelation.TYPE_FATHER_IN_LAW
            getString(R.string.relation_mother_in_law).lowercase() -> ContactRelation.TYPE_MOTHER_IN_LAW
            getString(R.string.relation_son_in_law).lowercase() -> ContactRelation.TYPE_SON_IN_LAW
            getString(R.string.relation_daughter_in_law).lowercase() -> ContactRelation.TYPE_DAUGHTER_IN_LAW
            getString(R.string.relation_brother_in_law).lowercase() -> ContactRelation.TYPE_BROTHER_IN_LAW
            getString(R.string.relation_sister_in_law).lowercase() -> ContactRelation.TYPE_SISTER_IN_LAW
            else -> ContactRelation.TYPE_CUSTOM
        }
        )
    } // ContactActivity.getRelationTypeFromText

    fun convertKnownRelationCustomTypesToIDs(relations: ArrayList<ContactRelation>) {
        relations.forEach { convertRelation ->
            if (convertRelation.type == ContactRelation.TYPE_CUSTOM) {
                val type = getRelationTypeFromText(convertRelation.label.trim())
                if (type != ContactRelation.TYPE_CUSTOM) {
                    convertRelation.type = type
                    convertRelation.label = ""
                }
            }
        }
    } // ContactActivity.convertKnownRelationCustomTypesToIDs()

    private fun getBigLetterPlaceholder(name: String): Bitmap {
        val letter = name.getNameLetter()
        val height = resources.getDimension(R.dimen.top_contact_image_height).toInt()
        val bitmap = Bitmap.createBitmap(realScreenSize.x, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val view = TextView(this)
        view.layout(0, 0, bitmap.width, bitmap.height)

        val circlePaint = Paint().apply {
            color = letterBackgroundColors[Math.abs(name.hashCode()) % letterBackgroundColors.size].toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val wantedTextSize = bitmap.height / 2f
        val textPaint = Paint().apply {
            color = circlePaint.color.getContrastColor()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = wantedTextSize
            style = Paint.Style.FILL
        }

        canvas.drawPaint(circlePaint)

        val xPos = canvas.width / 2f
        val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(letter, xPos, yPos, textPaint)
        view.draw(canvas)
        return bitmap
    }

    protected fun getDefaultRingtoneUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    protected fun getRingtonePickerIntent(): Intent {
        val defaultRingtoneUri = getDefaultRingtoneUri()
        val currentRingtoneUri = if (contact!!.ringtone != null && contact!!.ringtone!!.isNotEmpty()) {
            Uri.parse(contact!!.ringtone)
        } else if (contact!!.ringtone?.isNotEmpty() == false) {
            null
        } else {
            defaultRingtoneUri
        }

        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultRingtoneUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri)
        }
    }
}
