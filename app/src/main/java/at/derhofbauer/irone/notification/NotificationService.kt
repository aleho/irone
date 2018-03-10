/*
 * Copyright © 2018 Alexander Hofbauer
 *
 * This file is part of IronE.
 *
 * IronE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IronE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IronE.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.derhofbauer.irone.notification

import android.app.Notification
import android.content.ContentResolver
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import at.derhofbauer.irone.log.Log
import at.derhofbauer.irone.BuildConfig
import at.derhofbauer.irone.calendar.CalendarService

import java.util.ArrayList
import java.util.Calendar
import java.util.HashMap

import at.derhofbauer.irone.settings.AppSettingsManager
import at.derhofbauer.irone.settings.IroneSettingsManager
import java.text.DateFormat

class NotificationService internal constructor(
    notificationHandler: NotificationHandler,
    private val mPowerManager: PowerManager,
    mIroneSettings: IroneSettingsManager,
    private val mAppSettings: AppSettingsManager,
    private val mListener: NotificationListenerService
) {
    init {
        mIroneSettings.onChangeImmediate(IroneSettingsManager.PREF_DO_NOT_DISTURB) { value ->
            prefDoNotDisturb = value as Boolean
            Log.d(TAG, "do-not-disturb: $value")
        }

        mIroneSettings.onChangeImmediate(IroneSettingsManager.PREF_SCREEN_ON) { value ->
            prefScreenOn = value as Boolean
            Log.d(TAG, "screen-on: $value")
        }
    }

    companion object  {
        private const val TAG = "NotificationService"

        const val SETTINGS_INTENT = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

        // notifications for a group within a minute are simply nagging
        private const val GROUP_DELAY = 60 * 1000 //TODO: settings

        /**
         * Checks whether the user has allowed us to receive notifications.
         * @param contentResolver
         * @param packageName
         * @return
         */
        fun canAccessNotifications(contentResolver: ContentResolver, packageName: String): Boolean {
            val enabledApps = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")

            return enabledApps != null && enabledApps.contains(packageName)
        }
    }

    private var prefDoNotDisturb = false
    private var prefScreenOn     = false
    private var mNotificationHandler: NotificationHandler = notificationHandler

    private val mGroupTime = HashMap<String, Long>()

    /**
     * Queries the current do-not-disturb status.
     *
     * @return
     */
    private val currentInterruptionFilter: Int
        get() = mListener.currentInterruptionFilter


    internal fun setNotificationHandler(handler: NotificationHandler) {
        mNotificationHandler = handler
    }

    internal fun addTestNotification(): Int {
        val notification = AppNotification()
        with (notification) {
            primary   = "IronE"
            secondary = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Calendar.getInstance().time) + " 123456"
        }

        return mNotificationHandler.addNotification(notification)
    }

    internal fun handleNotification(sbn: StatusBarNotification) {
        debug(sbn)

        if (sbn.id < 0) {
            Log.d(TAG, "bogus negative id ${sbn.id}, skipping")
            return
        }

        if (sbn.isOngoing) {
            // only show "real" notifications
            Log.d(TAG, "isOngoing ${sbn.id}, skipping")
            return
        }

        val packageName = sbn.packageName

        if (packageName == "android") {
            Log.d(TAG, "android package, skipping ${sbn.id}")
            return
        }


        // always skip event notifications before checking settings
        if (isEventNotification(sbn, packageName)) {
            handleEventNotification(sbn)
            Log.d(TAG, "handled event notification i:${sbn.id}")
            return
        }


        if (prefDoNotDisturb && isInterruptionFiltered(sbn)) {
            Log.d(TAG, "interruption filtered ${mListener.currentInterruptionFilter} i:${sbn.id}")
            return
        }

        if (prefScreenOn && deviceIsInteractive()) {
            Log.d(TAG, "isInteractive, skipping i:${sbn.id}")
            // no need to send any notification (user is actively using the device)
            return
        }


        val setting = mAppSettings[packageName]

        if (setting == null || !setting.isEnabled) {
            Log.d(TAG, "won't handle notification, setting=${setting?.toString() ?: "null"}")
            return
        }


        if (isGroupAndShouldSkip(sbn.groupKey)) {
            Log.d(TAG, "isGroup and timeout not reached, skipping ${sbn.id}, ${sbn.groupKey}")
            return
        }


        try {
            val appNotification = NotificationExtractor.getAppNotification(sbn, setting)

            if (appNotification != null) {
                mNotificationHandler.addNotification(appNotification)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error building app notification", e)
        }

        cleanupNotificationGroups()
    }

    /**
     * Checks whether the devices is currently being actively used.
     * @return
     */
    private fun deviceIsInteractive(): Boolean {
        return mPowerManager.isInteractive
    }

    /**
     * Handles an
     * @param sbn
     * @param packageName
     * @return
     */
    private fun isEventNotification(sbn: StatusBarNotification, packageName: String): Boolean {
        val notification = sbn.notification

        return if (Notification.CATEGORY_EVENT != notification.category) {
            false
        } else packageName == "com.google.android.calendar" || packageName == "com.android.calendar"

        // TODO: handle other calendar notifications providers as well
    }

    /**
     * Handles an event notification, potentially cancelling it if it's ours.
     *
     * @param sbn
     */
    private fun handleEventNotification(sbn: StatusBarNotification) {
        val handler = mNotificationHandler as? CalendarService ?: return
        val notification = sbn.notification

        val title = notification.extras.getString(Notification.EXTRA_TITLE)
        val textSequence = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        val text = textSequence?.toString() ?: ""
        val uid = notification.extras.getString("UID")

        if (handler.isOwnEvent(uid, title, text)) {
            Log.d(TAG, "is our own event, removing notification ${sbn.id}")
            mListener.cancelNotification(sbn.key)
        }
    }


    /**
     * Checks whether we're in do-not-disturb-mode.
     *
     * @param sbn
     * @return
     */
    private fun isInterruptionFiltered(sbn: StatusBarNotification): Boolean {
        val status = currentInterruptionFilter

        when (status) {
            NotificationListenerService.INTERRUPTION_FILTER_ALL,
            NotificationListenerService.INTERRUPTION_FILTER_UNKNOWN -> {
                return false
            }

            NotificationListenerService.INTERRUPTION_FILTER_NONE -> {
                return true
            }

            NotificationListenerService.INTERRUPTION_FILTER_ALARMS -> {
                if (sbn.notification.category == Notification.CATEGORY_ALARM) {
                    Log.d(TAG, "notification is alarm, should probably not filter")
                }

                return true
            }

            NotificationListenerService.INTERRUPTION_FILTER_PRIORITY -> {
                //TODO find out priority, find out if notification is allowed
                return false
            }

            else -> Log.w(TAG, "unknown interruption filter $status")
        }

        return false
    }

    /**
     * Checks whether a notifications group is known and should now be skipped.
     *
     * @param groupKey
     * @return
     */
    private fun isGroupAndShouldSkip(groupKey: String?): Boolean {
        if (groupKey == null || groupKey.isEmpty()) {
            return false
        }

        val now = Calendar.getInstance().timeInMillis
        val lastShown = mGroupTime[groupKey]

        mGroupTime[groupKey] = now

        Log.d(TAG, "groupKey: $groupKey, lastShow: $lastShown")
        if (lastShown == null || lastShown <= 0) {
            return false
        }

        val timeout = lastShown + GROUP_DELAY

        Log.d(TAG, "timeout: $timeout, now: $now")

        return timeout > now
    }

    /**
     * Removes stale notification timeout information.
     */
    private fun cleanupNotificationGroups() {
        val now = Calendar.getInstance().timeInMillis

        for ((key, lastShown) in mGroupTime) {
            if (lastShown + GROUP_DELAY < now) {
                mGroupTime.remove(key)
            }
        }
    }

    /**
     * @param sbn Notification to debug
     */
    private fun debug(sbn: StatusBarNotification) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val notification = sbn.notification

        val extras = notification.extras.keySet()
        val lines = ArrayList<String>(9 + extras.size)

        lines.add("id=${sbn.id}")
        lines.add("key=${sbn.key}")
        lines.add("tag=${sbn.tag}")
        lines.add("pkg=${sbn.packageName}")
        lines.add("gid=${sbn.groupKey}")
        lines.add("ong=${sbn.isOngoing}")
        lines.add("cat=${notification.category}")
        lines.add("grp=${notification.group}")
        lines.add("tck=${notification.tickerText}")

        for (key in extras) {
            val value = notification.extras.get(key)

            lines.add("$key=${value?.toString() ?: "[null]"} | [%${if (value != null) value.javaClass.name else ""}]")
        }

        val builder = StringBuilder()
        for (line in lines) {
            builder.append(line).append("\n")
        }

        Log.d(TAG, builder.toString())
    }
}
