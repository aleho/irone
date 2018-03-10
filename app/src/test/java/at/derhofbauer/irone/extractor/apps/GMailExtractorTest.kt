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
import at.derhofbauer.irone.notification.extractor.apps.GMailExtractor

import org.junit.Assert.assertEquals

internal class GMailExtractorTest : ExtractorTest()
{
    @Test
    @Throws(Exception::class)
    fun test_appNotification()
    {
        val title = "Some subject - this is quite a long line"
        val message = "Hi there! THis is your message"
        val packageName = "com.google.android.apps.inbox"

        val extractor = GMailExtractor()
        val appSetting = getAppSetting(packageName, "GMail")

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_EMAIL,
            title,
            message,
            null, null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals(message, an!!.primary)
        assertEquals(title, an.secondary)

        assertEquals("Hi there! Some sub", an.string)
    }

    @Test
    @Throws(Exception::class)
    fun test_appNotificationShort()
    {
        val title = "Subject"
        val message = "Hi!"
        val packageName = "com.google.android.apps.inbox"

        val extractor = GMailExtractor()
        val appSetting = getAppSetting(packageName, "GMail")

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_EMAIL,
            title,
            message, null, null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals(message, an!!.primary)
        assertEquals(title, an.secondary)

        assertEquals("Hi! G: Subject", an.string)
    }
}
