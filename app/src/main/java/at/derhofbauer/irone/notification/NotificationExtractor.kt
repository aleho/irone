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

import android.service.notification.StatusBarNotification
import at.derhofbauer.irone.log.Log

import at.derhofbauer.irone.notification.extractor.ExtractorFactory
import at.derhofbauer.irone.settings.AppSetting

internal object NotificationExtractor
{
    private const val TAG = "NotificationExtractor"

    fun getAppNotification(sbn: StatusBarNotification, setting: AppSetting): AppNotification?
    {
        val packageName = sbn.packageName

        val extractor = ExtractorFactory.getExtractor(packageName)
        var appNotification: AppNotification? = null

        if (extractor != null) {
            Log.d(TAG, "package extractor " + extractor.javaClass.name)
            appNotification = extractor.extract(sbn, setting)
        }

        if (appNotification == null) {
            Log.d(TAG, "fallback extractor")
            appNotification = ExtractorFactory.fallback.extract(sbn, setting)
        }

        Log.d(TAG, "an: " + appNotification)

        return appNotification
    }
}
