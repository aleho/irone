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
import at.derhofbauer.irone.notification.extractor.apps.TwitterExtractor

import org.junit.Assert.assertEquals

internal class TwitterExtractorTest : ExtractorTest()
{
    @Test
    @Throws(Exception::class)
    fun test_appNotification()
    {
        val message = "RT @handle: This is some text and then some more and very long."
        val title = "Userfirst Userlast"
        val packageName = "com.twitter.android"

        val extractor = TwitterExtractor()
        val appSetting = getAppSetting(packageName, "Reddit")

        val sbn = MockStatusBarNotification.build(
            packageName,
            null,
            title,
            message, null, null
        )

        val an = extractor.extract(sbn, appSetting)
        assertEquals(title, an!!.from)
        assertEquals(message, an.primary)

        assertEquals("User: RT @handle:", an.string)
    }
}
