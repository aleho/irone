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

package at.derhofbauer.irone.extractor.apps

import android.app.Notification

import org.junit.Test

import at.derhofbauer.irone.MockStatusBarNotification
import at.derhofbauer.irone.notification.extractor.apps.FallbackExtractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

internal class FallbackExtractorTest : ExtractorTest()
{
    @Test
    @Throws(Exception::class)
    fun test_appNotification()
    {
        val title = "New Notifications"
        val text = "There are new notifications for this app"
        val packageName = "org.someapp.unknown"
        val label = "Fallback"

        val extractor = FallbackExtractor()
        val appSetting = getAppSetting(packageName, label)

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            null, null
        )

        val an = extractor.extract(sbn, appSetting)
        assertNull(an!!.from)
        assertNull(an.secondary)
        assertEquals(label, an.primary)

        assertEquals("Fallback", an.string)
    }
}
