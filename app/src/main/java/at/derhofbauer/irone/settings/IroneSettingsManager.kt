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

package at.derhofbauer.irone.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import at.derhofbauer.irone.log.Log

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class IroneSettingsManager private constructor(context: Context)
{
    companion object {
        private const val TAG = "IroneSettingsManager"

        const val PREF_ENABLED        = "pref_enabled"
        const val PREF_DO_NOT_DISTURB = "pref_do_not_disturb"
        const val PREF_SCREEN_ON      = "pref_screen_on"
        const val PREF_NOTIFIER       = "pref_notifier"

        const val NOTIFIER_CALENDAR  = 0
        const val NOTIFIER_BLUETOOTH = 1

        private val sListeners: ConcurrentHashMap<String, ConcurrentLinkedQueue<Callback<*>>> = ConcurrentHashMap()
        private lateinit var mPrefs: SharedPreferences

        @Volatile
        private var sInstance: IroneSettingsManager? = null

        fun getInstance(context: Context): IroneSettingsManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: IroneSettingsManager(context).also { sInstance = it }
            }
        }

        private val mOnPrefChangedListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (sListeners.isEmpty()) {
                return@OnSharedPreferenceChangeListener
            }

            val callbacks = sListeners[key] ?: return@OnSharedPreferenceChangeListener

            if (callbacks.isEmpty()) {
                return@OnSharedPreferenceChangeListener
            }

            val value = sharedPreferences.all[key] ?: return@OnSharedPreferenceChangeListener

            for (callback in callbacks) {
                callback.exec(value)
            }
        }
    }

    private class Callback<in T>(internal val func: (T) -> Unit) {
        internal fun exec(value: Any) {
            func.invoke(value as T)
        }
    }

    private var editor: Editor? = null
        @SuppressLint("CommitPrefEdits")
        get() {
            if (field == null) {
                field = mPrefs.edit()
            }

            return field
        }


    var enabled: Boolean
        get() = mPrefs.getBoolean(PREF_ENABLED, false)
        set(enabled) {
            editor?.putBoolean(PREF_ENABLED, enabled)
        }

    var doNotDisturb: Boolean
        get() = mPrefs.getBoolean(PREF_DO_NOT_DISTURB, false)
        set(enabled) {
            editor?.putBoolean(PREF_DO_NOT_DISTURB, enabled)
        }

    var screenOn: Boolean
        get() = mPrefs.getBoolean(PREF_SCREEN_ON, false)
        set(enabled) {
            editor?.putBoolean(PREF_SCREEN_ON, enabled)
        }

    var notifier: Int
        get() = mPrefs.getString(PREF_NOTIFIER, "0").toInt()
        set(value) {
            editor?.putString(PREF_NOTIFIER, value.toString())
        }


    init {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun save() {
        editor?.apply()
        editor = null
    }


    /**
     * Registers a change listener.
     */
    fun <T> onChange(pref: String, listener: (T) -> Unit) {
        if (sListeners.isEmpty()) {
            mPrefs.registerOnSharedPreferenceChangeListener(mOnPrefChangedListener)
        }

        var queue = sListeners[pref]
        if (queue == null) {
            queue = ConcurrentLinkedQueue()
            sListeners[pref] = queue
        }

        for (item in queue) {
            if (item.func === listener) {
                Log.d(TAG, "listener already registered for $pref ($listener)")
                return
            }
        }

        queue.offer(Callback(listener))
    }

    /**
     * Registers a change listener and executes the listener immediately with its current value.
     */
    fun <T> onChangeImmediate(pref: String, listener: (T) -> Unit) {
        mPrefs.all[pref]?.let { listener.invoke(it as T) }

        onChange(pref, listener)
    }

    /**
     * Removes all references to a listener
     */
    fun <T> removeListener(listener: (T) -> Unit) {
        for ((pref, queue) in sListeners) {
            for (item in queue) {
                if (item.func === listener) {
                    Log.d(TAG, "listener removed for $pref ($listener)")
                    queue.remove(item)
                }
            }
        }
    }
}
