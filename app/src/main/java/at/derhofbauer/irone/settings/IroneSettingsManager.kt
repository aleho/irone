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

import java.util.HashMap
import java.util.concurrent.ConcurrentLinkedQueue

class IroneSettingsManager private constructor(context: Context)
{
    companion object {
        const val PREF_ENABLED        = "pref_enabled"
        const val PREF_DO_NOT_DISTURB = "pref_do_not_disturb"
        const val PREF_SCREEN_ON      = "pref_screen_on"
        const val PREF_NOTIFIER       = "pref_notifier"

        const val NOTIFIER_CALENDAR  = 0
        const val NOTIFIER_BLUETOOTH = 1


        private val sListeners: HashMap<String, ConcurrentLinkedQueue<(Any) -> Unit>> = HashMap()
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
                callback.invoke(value)
            }
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

    //TODO: add types and convert here
    fun onChange(pref: String, callback: (Any) -> Unit) {
        if (sListeners.isEmpty()) {
            mPrefs.registerOnSharedPreferenceChangeListener(mOnPrefChangedListener)
        }

        var queue = sListeners[pref]
        if (queue == null) {
            queue = ConcurrentLinkedQueue()
            sListeners[pref] = queue
        }

        if (!queue.contains(callback)) {
            queue.offer(callback)
        }
    }

    /**
     * Registers a change listener and executes the callback immediately with its current value.
     */
    fun onChangeImmediate(pref: String, callback: (Any) -> Unit) {
        mPrefs.all[pref]?.let { callback.invoke(it) }

        onChange(pref, callback)
    }
}
