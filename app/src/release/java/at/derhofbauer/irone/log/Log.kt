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

package at.derhofbauer.irone.log

import android.util.Log as ALog

@Suppress("unused", "UNUSED_PARAMETER")
/**
 * Logging util. Only errors are logged.
 */
class Log {
    companion object {
        fun e(tag: String, msg: String): Int {
            return ALog.e(tag, msg)
        }

        fun e(tag: String, msg: String, tr: Throwable): Int {
            return ALog.e(tag, msg, tr)
        }

        fun v(tag: String, msg: String): Int {
            return 0
        }

        fun v(tag: String, msg: String, tr: Throwable): Int {
            return 0
        }

        fun d(tag: String, msg: String): Int {
            return 0
        }

        fun d(tag: String, msg: String, tr: Throwable): Int {
            return 0
        }

        fun i(tag: String, msg: String): Int {
            return 0
        }

        fun i(tag: String, msg: String, tr: Throwable): Int {
            return 0
        }

        fun w(tag: String, msg: String): Int {
            return 0
        }

        fun w(tag: String, msg: String, tr: Throwable): Int {
            return 0
        }
    }
}
