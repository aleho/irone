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

import android.os.Environment
import java.io.File
import java.text.DateFormat
import java.util.*
import android.util.Log as ALog

/**
 * Logging util. Logs to a file as well to Android log.
 */
class Log {
    companion object {
        private const val TAG  = "Log"
        private const val FILE = "irone.log"

        private var logfile: File? = null

        private fun getLogFile(): File? {
            var file = logfile

            if (file != null) {
                return file
            }

            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                ALog.e(TAG, "external storage is not mounted")
                return null
            }

            try {
                file = File(Environment.getExternalStorageDirectory(), FILE)

                if (!file.isFile) {
                    file.createNewFile()

                    if (!file.isFile) {
                        ALog.e(TAG, "could not create log file $FILE")
                        return null
                    }

                    ALog.d(TAG, "created log file $FILE")
                }

                if (!file.canWrite()) {
                    ALog.e(TAG, "log file $FILE is not writable")
                    return null
                }

                logfile = file
                return file

            } catch (e: Exception) {
                ALog.e(TAG, "error getting log file $FILE", e)
            }

            return null
        }

        private fun writeLog(level: Int, tag: String, msg: String, tr: Throwable?): Boolean {
            val file = getLogFile() ?: return false

            val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Calendar.getInstance().timeInMillis)

            file.appendText("\n[")
            file.appendText(timestamp)
            file.appendText("] | ")

            when (level) {
                ALog.VERBOSE -> file.appendText("VERBOSE")
                ALog.DEBUG   -> file.appendText("DEBUG")
                ALog.INFO    -> file.appendText("INFO")
                ALog.WARN    -> file.appendText("WARN")
                ALog.ERROR   -> file.appendText("ERROR")
                else         -> file.appendText("UNKNOWN")
            }

            file.appendText(" | ")
            file.appendText(tag)
            file.appendText(" | ")
            file.appendText(msg)

            if (tr != null) {
                file.appendText("\n")
                file.appendText(ALog.getStackTraceString(tr))
            }

            return true
        }

        private fun log(level: Int, tag: String, msg: String, tr: Throwable?): Int {
            val ret = when (level) {
                ALog.DEBUG   -> if (tr != null) ALog.d(tag, msg, tr) else ALog.d(tag, msg)
                ALog.INFO    -> if (tr != null) ALog.i(tag, msg, tr) else ALog.i(tag, msg)
                ALog.WARN    -> if (tr != null) ALog.w(tag, msg, tr) else ALog.w(tag, msg)
                ALog.ERROR   -> if (tr != null) ALog.e(tag, msg, tr) else ALog.e(tag, msg)
                else         -> if (tr != null) ALog.v(tag, msg, tr) else ALog.v(tag, msg)
            }

            if (!writeLog(level, tag, msg, tr)) {
                ALog.e(TAG, "Could not write to log file $FILE")
            }

            return ret
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
