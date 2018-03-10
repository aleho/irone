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

package at.derhofbauer.irone.bluetooth.comm

import at.derhofbauer.irone.BuildConfig
import at.derhofbauer.irone.log.Log

internal abstract class Communication
{
    internal fun debug(data: ByteArray) {
        if (!BuildConfig.DEBUG) {
            return
        }

        debug(data.asIterable())
    }

    /**
     * Debug helper for data being sent.
     */
    internal fun debug(data: Iterable<Byte>) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val debugOutput = StringBuffer()
        for (byte in data) {
            debugOutput.append("0x%02X ".format(byte))
        }

        Log.d(getLogTag(), "data: ${data.count()}: $debugOutput")
    }

    internal abstract fun getLogTag(): String
}
