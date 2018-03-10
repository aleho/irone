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
import at.derhofbauer.irone.notification.extractor.apps.SignalExtractor

import org.junit.Assert.assertEquals

internal class SignalExtractorTest : ExtractorTest()
{
    @Test
    @Throws(Exception::class)
    fun test_appNotification()
    {
        val title = "Alex Sender"
        val message = "This is a message"
        val packageName = "org.thoughtcrime.securesms"

        val extractor = SignalExtractor()
        val appSetting = getAppSetting(packageName, "Signal")

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            message,
            null, null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals(title, an!!.from)
        assertEquals(message, an.primary)

        assertEquals("Alex: This is a me", an.string)
    }
}
