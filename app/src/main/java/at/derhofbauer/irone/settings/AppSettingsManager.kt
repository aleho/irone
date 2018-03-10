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

import android.content.Context
import android.content.SharedPreferences
import at.derhofbauer.irone.log.Log

import java.util.HashMap

open class AppSettingsManager private constructor(context: Context)
{
    companion object
    {
        private const val TAG = "AppSettingsManager"
        private const val PREFS_NAME = "app_settings"

        @Volatile
        private var sInstance: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager
        {
            return sInstance ?: synchronized(this) {
                sInstance ?: AppSettingsManager(context).also { sInstance = it }
            }
        }
    }

    private val mPrefs: SharedPreferences
    private val mApps: MutableMap<String, AppSetting> = HashMap()

    init
    {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        load()
    }

    private fun load()
    {
        for ((key, value) in mPrefs.all) {
            val setting = AppSetting.fromString(value as String, this)
            mApps[key] = setting
        }
    }

    operator fun get(packageName: String): AppSetting?
    {
        return mApps[packageName]
    }

    /**
     * Loads or initializes an app setting.
     *
     * @param packageName PackageManager package name
     * @param defaultLabel Default label for the app setting if unknown
     *
     * @return An existing or new instance of app setting
     */
    fun getOrInit(packageName: String, defaultLabel: String): AppSetting
    {
        var setting = get(packageName)

        if (setting == null) {
            setting = AppSetting(this, packageName)
            mApps[packageName] = setting
        }

        if (setting.label == null) {
            setting.label = defaultLabel
        }

        return setting
    }

    internal fun save(setting: AppSetting)
    {
        Log.d(TAG, "Saving " + setting)

        val packageName = setting.packageName

        if (get(packageName) == null) {
            Log.w(TAG, "Setting not found " + packageName)
            mApps[packageName] = setting
        }

        mPrefs.edit()
            .putString(setting.packageName, setting.toString())
            .apply()
    }
}
