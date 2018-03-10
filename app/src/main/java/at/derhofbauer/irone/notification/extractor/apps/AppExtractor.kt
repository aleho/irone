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

package at.derhofbauer.irone.notification.extractor.apps

import android.app.Notification
import android.service.notification.StatusBarNotification
import at.derhofbauer.irone.log.Log

import at.derhofbauer.irone.notification.AppNotification
import at.derhofbauer.irone.notification.extractor.Extractor
import at.derhofbauer.irone.settings.AppSetting

abstract class AppExtractor : Extractor
{
    companion object
    {
        private const val TAG = "AppExtractor"
    }

    override fun extract(sbn: StatusBarNotification, setting: AppSetting): AppNotification?
    {
        val notification = sbn.notification

        if (!supportsCategory(notification.category)) {
            Log.d(TAG, "category not supported, " + notification.category + " by " + this.javaClass.name)
            return null
        }

        val an = AppNotification()
        an.application = setting.label

        doExtract(an, notification)

        // a primary text to display is sufficient
        if (an.primary?.isNotEmpty() == true) {
            return an
        }

        // just a few fallback tries
        Log.d(TAG, "no primary text extracted, fallbacks")

        an.primary = getText(notification)
        if (an.primary?.isNotEmpty() == true) {
            return an
        }

        an.primary = getTitle(notification)
        if (an.primary?.isNotEmpty() == true) {
            return an
        }

        an.primary = getSummary(notification)
        if (an.primary?.isNotEmpty() == true) {
            return an
        }

        an.primary = getSubText(notification)
        if (an.primary?.isNotEmpty() == true) {
            return an
        }

        return an
    }

    fun getTitle(notification: Notification): String?
    {
        return notification.extras.getString(Notification.EXTRA_TITLE)
    }

    fun getText(notification: Notification): String?
    {
        val textSequence = notification.extras.getCharSequence(Notification.EXTRA_TEXT)

        return textSequence?.toString()
    }

    fun getSubText(notification: Notification): String?
    {
        return notification.extras.getString(Notification.EXTRA_SUB_TEXT)
    }

    fun getSummary(notification: Notification): String?
    {
        return notification.extras.getString(Notification.EXTRA_SUMMARY_TEXT)
    }

    internal abstract fun doExtract(an: AppNotification, notification: Notification)

    internal abstract fun supportsCategory(category: String?): Boolean
}
