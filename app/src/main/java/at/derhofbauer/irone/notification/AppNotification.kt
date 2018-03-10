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

import at.derhofbauer.irone.bluetooth.BluetoothService
import com.google.gson.Gson
import com.vdurmont.emoji.EmojiParser

class AppNotification
{
    companion object {
        private const val FROM_MAX_LENGTH = 4
        private const val TEXT_MAX_LENGTH = BluetoothService.TEXT_MAX_LENGTH
    }

    var application: String? = null
    var from: String? = null
    var primary: String? = null
    var secondary: String? = null

    val string: String
        get() {
            return buildString()
        }

    val strings: Array<String>
        get() {
            return buildStrings()
        }

    /**
     * Builds a concatenated string for this application notification.
     */
    private fun buildString(): String {
        val strings = buildStrings()

        return truncateTrim("${strings[0]} ${strings[1]}", TEXT_MAX_LENGTH)
    }

    /**
     * Builds the main and secondary string for this application notification.
     *
     * @return Strings
     */
    private fun buildStrings(): Array<String> {
        var from = clearString(this.from)
        var primary = clearString(this.primary)
        var secondary = clearString(this.secondary)
        var app = clearString(this.application)

        val fromLen = from.length
        var primaryLen = primary.length
        var secondaryLen = secondary.length
        val appLen = app.length

        if (fromLen > 0) {
            if (fromLen > FROM_MAX_LENGTH) {
                from = from.substring(0, FROM_MAX_LENGTH)
            }
            primary = truncateTrim("$from: $primary", TEXT_MAX_LENGTH)
        }

        if (primaryLen > 9 && secondaryLen > 8) {
            primary = truncateTrim(primary, 9)
            secondary = truncateTrim(secondary, 8)
            primaryLen = primary.length
            secondaryLen = secondary.length
        }

        val appPlusSecondary = appLen + secondaryLen + 2
        val charsLeft = TEXT_MAX_LENGTH - (primaryLen + appPlusSecondary)

        if (charsLeft > 0 && appLen > 0) {
            if (secondaryLen > 0) {
                if (appPlusSecondary > charsLeft) {
                    app = app.substring(0, charsLeft)
                }
                secondary = "$app: $secondary"

            } else {
                secondary = app
            }

            secondary = truncateTrim(secondary, TEXT_MAX_LENGTH)
        }

        return arrayOf(primary, secondary)
    }

    /**
     *
     */
    private fun truncateTrim(string: String?, length: Int): String {
        if (string?.isEmpty() != false) {
            return ""
        }

        var ret = string

        if (ret.length > length) {
            ret = ret.substring(0, length)
        }

        return ret.trim()
    }

    /**
     * Removes all Emojis (they're not supported by HealthMate).
     * Replaces new lines with spaces.
     * Replaces multiple spaces with single space.
     * Truncates at AppNotification::TEXT_MAX_LENGTH.
     * Finally trims all whitespace.
     *
     * @param string Long string potentially containing emojis and other unwanted characters
     * @return String guaranteed to be without emojis and short
     */
    private fun clearString(string: String?): String {
        var ret = string
        if (ret == null || ret.isEmpty()) {
            return ""
        }

        ret = EmojiParser.removeAllEmojis(ret)
            .trim()
            .replace("\\s+".toRegex(), " ")

        ret = truncateTrim(ret, TEXT_MAX_LENGTH)

        return ret
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}
