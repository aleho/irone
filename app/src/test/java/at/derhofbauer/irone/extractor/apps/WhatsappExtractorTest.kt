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

import org.junit.Test

import at.derhofbauer.irone.MockStatusBarNotification
import at.derhofbauer.irone.notification.extractor.apps.WhatsappExtractor

import org.junit.Assert.assertEquals

internal class WhatsappExtractorTest : ExtractorTest()
{
    @Test
    fun test_appNotification()
    {
        val title = "Firstname Lastname"
        val text = "This is a message very long message and should be shown after from"
        val packageName = "com.whatsapp"

        val extractor = WhatsappExtractor()
        val appSetting = getAppSetting(packageName, "WhatsApp")

        val sbn = MockStatusBarNotification.build(
            packageName,
            null, //Notification.CATEGORY_MESSAGE, did what's app change that?
            title,
            text,
            null,
            null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals("Firstname", an!!.from)
        assertEquals(text, an.primary)

        assertEquals("Firs: This is a me", an.string)
    }

    @Test
    fun test_appNotification_shortFrom()
    {
        val title = "Sho"
        val text = "This is a message very long message and should be shown after from"
        val packageName = "com.whatsapp"

        val extractor = WhatsappExtractor()
        val appSetting = getAppSetting(packageName, "WhatsApp")

        val sbn = MockStatusBarNotification.build(
            packageName,
            null, //Notification.CATEGORY_MESSAGE, did what's app change that?
            title,
            text,
            null,
            null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals("Sho", an!!.from)
        assertEquals(text, an.primary)

        assertEquals("Sho: This is a mes", an.string)
    }
}
