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

package at.derhofbauer.irone.notification.extractor

import at.derhofbauer.irone.log.Log

import java.util.HashMap

import at.derhofbauer.irone.notification.extractor.apps.FallbackExtractor
import at.derhofbauer.irone.notification.extractor.apps.GMailExtractor
import at.derhofbauer.irone.notification.extractor.apps.InboxExtractor
import at.derhofbauer.irone.notification.extractor.apps.RedditExtractor
import at.derhofbauer.irone.notification.extractor.apps.SignalExtractor
import at.derhofbauer.irone.notification.extractor.apps.TelegramExtractor
import at.derhofbauer.irone.notification.extractor.apps.TwitterExtractor
import at.derhofbauer.irone.notification.extractor.apps.WhatsappExtractor

object ExtractorFactory
{
    private const val TAG = "ExtractorFactory"

    private const val PKG_GMAIL = "com.google.android.gm"
    private const val PKG_INBOX = "com.google.android.apps.inbox"
    private const val PKG_REDDIT = "com.reddit.frontpage"
    private const val PKG_SIGNAL = "org.thoughtcrime.securesms"
    private const val PKG_TELEGRAM = "org.telegram.messenger"
    private const val PKG_TWITTER = "com.twitter.android"
    private const val PKG_WHATSAPP = "com.whatsapp"

    val fallback: Extractor = FallbackExtractor()

    private val sExtractors = object : HashMap<String, Class<*>>()
    {
        init
        {
            put(PKG_GMAIL, GMailExtractor::class.java)
            put(PKG_INBOX, InboxExtractor::class.java)
            put(PKG_REDDIT, RedditExtractor::class.java)
            put(PKG_SIGNAL, SignalExtractor::class.java)
            put(PKG_TELEGRAM, TelegramExtractor::class.java)
            put(PKG_TWITTER, TwitterExtractor::class.java)
            put(PKG_WHATSAPP, WhatsappExtractor::class.java)
        }
    }

    private val sInstances = HashMap<String, Extractor>()


    fun getExtractor(packageName: String): Extractor?
    {
        if (!sExtractors.containsKey(packageName)) {
            return null
        }

        var extractor: Extractor? = sInstances[packageName]

        if (extractor == null) {
            try {
                val extractorClass = sExtractors[packageName]
                extractor = extractorClass?.newInstance() as Extractor
                sInstances[packageName] = extractor

            } catch (e: InstantiationException) {
                Log.e(TAG, "Error getting extractor instance", e)
                return null

            } catch (e: IllegalAccessException) {
                Log.e(TAG, "Error getting extractor instance", e)
                return null
            }

        }

        return extractor
    }
}
