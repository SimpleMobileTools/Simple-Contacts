Changelog
==========

Version 6.1.0 *(2018-11-30)*
----------------------------

 * Allow setting the app as the default for handling calls
 * Allow blocking numbers on Android 7+
 * Improved contact recognition at Recents
 * Fixed some handling related to creating new contact with a number, or adding a number to an existing contact
 * Add handling for secret codes like *#*#4636#*#*
 * Allow copying all contact fields from the view screen by long pressing them
 * Added an option for hiding the dialpad button on the main screen
 * Fixed some issues related to importing contacts with birthdays

Version 6.0.0 *(2018-11-06)*
----------------------------

 * Initial Pro version

Version 5.1.2 *(2018-11-28)*
----------------------------

 * Had to remove the Recents tab due to the latest Googles' permissions policy. The Pro app will be updated with a Recents tab and a mandatory Dialer soon.
 * This version of the app is no longer maintained, please upgrade to the Pro version. You can find the Upgrade button at the top of the app Settings.

Version 5.1.1 *(2018-11-05)*
----------------------------

 * Added some stability and translation improvements

Version 5.1.0 *(2018-10-28)*
----------------------------

 * Added an option for toggling 24 hour time format
 * Added an option to show only contacts with phone numbers
 * Do not allow creating contacts under WhatsApp contact source
 * Try fixing the issue with disappearing contacts under some circumstances
 * Fix some bugs related to selecting contact photo from the device storage
 * Show the contact email at the list, if name and organization are empty
 * Properly sort groups containing UTF-8 characters at title
 * Use a different placeholder image for business contacts

Version 5.0.0 *(2018-10-15)*
----------------------------

 * Increased the minimal required Android OS version to 5
 * Try fixing the issue with disappearing fresh contacts

Version 4.5.0 *(2018-09-28)*
----------------------------

 * Added a simple dialpad
 * Do not allow creating contacts under Signal or Telegram contact source
 * Allow copying phone numbers into clipboard by long pressing them on the View screen
 * Properly handle intents adding numbers to existing contacts
 * Many other smaller improvements and bugfixes

Version 4.4.0 *(2018-09-04)*
----------------------------

 * Added support for custom phone number/email/address types
 * Added IM field
 * Fixed some exporting/importing issues
 * Improved duplicate filtering

Version 4.3.0 *(2018-08-24)*
----------------------------

 * Reworked contact exporting/importing from scratch, use ez-vcard for parsing files
 * Couple stability and translation improvements

Version 4.2.2 *(2018-08-13)*
----------------------------

 * Added an optional Nickname field
 * Improved searching and sorting UTF8 characters
 * Fixed updating Notes and Organization fields

Version 4.2.1 *(2018-08-05)*
----------------------------

 * Added some stability and light theme UX fixes

Version 4.2.0 *(2018-08-04)*
----------------------------

 * Added a Recent Calls tab
 * Allow customizing which tabs are visible
 * Added an optional call confirmation dialog
 * Fixed some glitches related to company contacts
 * Some other performance and stability improvements

Version 4.1.0 *(2018-07-16)*
----------------------------

 * Fixed a couple issues related to importing contacts from .vcf files
 * Couple other UX and stability improvements

Version 4.0.5 *(2018-07-05)*
----------------------------

 * Make duplicate contact filtering more agressive
 * Couple UX and stability improvements

Version 4.0.4 *(2018-06-19)*
----------------------------

 * Make "Try filtering out duplicate contacts" more agressive
 * Ignore hidden contact fields, do not wipe them
 * Prefer the contacts Mobile number at sending batch SMS
 * Added a couple stability improvements

Version 4.0.3 *(2018-05-13)*
----------------------------

 * Show a couple additional contact sources

Version 4.0.2 *(2018-05-12)*
----------------------------

 * Make sure all relevant contact sources are visible

Version 4.0.1 *(2018-05-09)*
----------------------------

 * Fixed a glitch happening at updating from old app version to 4.x

Version 4.0.0 *(2018-05-09)*
----------------------------

 * Allow changing app icon color
 * Add a toggle for trying to filter out duplicate contacts, enabled by default
 * Fix some contacts not being visible
 * Allow opening contacts with third party apps
 * Couple misc fixes related to contacts syncing via CardDAV
 * Allow moving contacts in a different contact source at the Edit screen

Version 3.5.3 *(2018-04-18)*
----------------------------

 * Allow splitting the Address in multiple lines
 * Allow batch sending SMS or emails to selected contacts or whole group
 * Highlight the searched string at the result contacts

Version 3.5.2 *(2018-04-13)*
----------------------------

 * Added an optional website field
 * Parse more fields from Add Contact third party intents, like the one from WhatsApp
 * Fix exporting contacts on SD cards

Version 3.5.1 *(2018-04-10)*
----------------------------

 * Show the organization name as the contact name in case only that is filled
 * Fix "Start name with surname"

Version 3.5.0 *(2018-04-08)*
----------------------------

 * Added name prefix/suffix and contact organizations (hidden by default)
 * Added a settings item "Manage shown contact fields" for customizing visible contact details, with some fields disabled by default
 * Allow using the app without granting Contacts permission, rely on local secret storage only
 * Dial the selected contact if Call permission is not granted
 * Many performance improvements and smaller bugfixes

Version 3.4.0 *(2018-03-21)*
----------------------------

 * Added groups
 * Make phone numbers, emails and addresses clickable on the view screen
 * Many smaller improvements and bugfixes

Version 3.3.3 *(2018-03-04)*
----------------------------

 * Make sure Search searches address and Notes too

Version 3.3.2 *(2018-03-04)*
----------------------------

 * Some stability and translation improvements

Version 3.3.1 *(2018-03-02)*
----------------------------

 * Couple stability and translation improvements

Version 3.3.0 *(2018-02-22)*
----------------------------

 * Added Address and Notes fields
 * Properly handle Get Email intent
 * Fixed some glitches at exporting contacts
 * Added FAQ

Version 3.2.0 *(2018-02-11)*
----------------------------

 * Allow storing contacts in a local database, hidden from other apps
 * Added a new screen for viewing contact details
 * Increased font size

Version 3.1.4 *(2018-02-03)*
----------------------------

 * Add a unified "Phone storage" label to local phone storage
 * Properly handle special characters at contact ex/importing

Version 3.1.3 *(2018-01-29)*
----------------------------

 * Allow hiding contact thumbnails
 * Fix displaying and adding contacts on some devices

Version 3.1.2 *(2018-01-23)*
----------------------------

 * Properly handle vcf files exported from Thunderbird
 * Misc smaller improvements

Version 3.1.1 *(2018-01-22)*
----------------------------

 * An f-droid build test version

Version 3.1.0 *(2018-01-16)*
----------------------------

 * Added contact import/export functions
 * Allow sharing contacts

Version 3.0.3 *(2018-01-03)*
----------------------------

 * Make the contacts beginning with the search string appear first
 * Some crashfixes

Version 3.0.2 *(2018-01-02)*
----------------------------

 * Handle some third party intents
 * Reuse the shared Favorite contacts, don't create an own list

Version 3.0.1 *(2018-01-01)*
----------------------------

 * Initial release
