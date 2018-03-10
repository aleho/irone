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

import com.google.gson.Gson

open class AppSetting internal constructor(
    @field:Transient private var mManager: AppSettingsManager,
    open val packageName: String
) {
    @Transient
    private var mHasChanges = false

    open var label: String? = null
        /**
         * @param label Sets the display label for this app setting.
         * @return this
         */
        set(label) {
            if (label != field) {
                mHasChanges = true
            }

            field = label
        }

    open var isEnabled = false
        /**
         * @param enabled Sets the enable status (whether to show messages from this app).
         * @return this
         */
        set(enabled) {
            if (enabled != field) {
                mHasChanges = true
            }

            field = enabled
        }

    fun persist()
    {
        if (!mHasChanges) {
            return
        }

        mHasChanges = false
        mManager.save(this)
    }

    /**
     * @return A JSON representation of this settings class.
     */
    override fun toString(): String
    {
        return sGson.toJson(this)
    }

    companion object
    {
        @Transient
        private val sGson = Gson()

        /**
         * Converts a JSON string to an app setting.
         * @param string JSON
         * @return The deserialized app setting
         */
        fun fromString(string: String, manager: AppSettingsManager): AppSetting
        {
            val appSetting = sGson.fromJson(string, AppSetting::class.java)
            appSetting.mManager = manager

            return appSetting
        }
    }
}
