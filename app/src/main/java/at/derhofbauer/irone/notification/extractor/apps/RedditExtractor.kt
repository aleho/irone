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

import at.derhofbauer.irone.notification.AppNotification

/**
 * Reddit
 *
 *
 * Example:
 * android.title=u/username commented in "Post name" | [java.lang.String]
 * android.text=Some comment content | [java.lang.String]
 */
internal class RedditExtractor : AppExtractor()
{
    companion object
    {
        private const val USERNAME_PREFIX = "u/"
    }

    override fun doExtract(an: AppNotification, notification: Notification)
    {
        val title = getTitle(notification)

        if (title != null
            && title.startsWith(USERNAME_PREFIX)
            && title.length > USERNAME_PREFIX.length) {
            an.from = title.split(" ", limit = 2)[0].substring(2)
            an.primary = getText(notification)
            an.application = null

        } else {
            an.primary = title
        }
    }

    override fun supportsCategory(category: String?): Boolean
    {
        // reddit doesn't set a category
        return true
    }
}
