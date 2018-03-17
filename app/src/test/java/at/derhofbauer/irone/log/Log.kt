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

/**
 * Stdout logging in tests.
 */
class Log {
    companion object {
        private fun log(level: Int, tag: String, msg: String, tr: Throwable?): Int {
            val lvl = when (level) {
                ALog.DEBUG -> "DEBUG"
                ALog.INFO  -> "INFO"
                ALog.WARN  -> "WARN"
                ALog.ERROR -> "ERROR"
                else       -> "VERBOSE"
            }

            println("$lvl | $tag | $msg")

            if (tr != null) {
                println(tr)
            }

            return 0
        }

        fun v(tag: String, msg: String): Int {
            return log(ALog.VERBOSE, tag, msg, null)
        }

        fun v(tag: String, msg: String, tr: Throwable): Int {
            return log(ALog.VERBOSE, tag, msg, tr)
        }

        fun d(tag: String, msg: String): Int {
            return log(ALog.DEBUG, tag, msg, null)
        }

        fun d(tag: String, msg: String, tr: Throwable): Int {
            return log(ALog.DEBUG, tag, msg, tr)
        }

        fun i(tag: String, msg: String): Int {
            return log(ALog.INFO, tag, msg, null)
        }

        fun i(tag: String, msg: String, tr: Throwable): Int {
            return log(ALog.INFO, tag, msg, tr)
        }

        fun w(tag: String, msg: String): Int {
            return log(ALog.WARN, tag, msg, null)
        }

        fun w(tag: String, msg: String, tr: Throwable): Int {
            return log(ALog.WARN, tag, msg, tr)
        }

        fun e(tag: String, msg: String): Int {
            return log(ALog.ERROR, tag, msg, null)
        }

        fun e(tag: String, msg: String, tr: Throwable): Int {
            return log(ALog.ERROR, tag, msg, tr)
        }
    }
}
