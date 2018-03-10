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

package at.derhofbauer.irone.appslist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat

import at.derhofbauer.irone.settings.AppSetting
import at.derhofbauer.irone.settings.AppSettingsManager

class AppEntry internal constructor(
    private val mSettingsManager: AppSettingsManager,
    private val mInfo: ApplicationInfo
) {
    private var mLabel: String? = null
    private var mIcon: Drawable? = null
    private var mAppSetting: AppSetting? = null

    /**
     * @param packageManager
     * @return
     */
    internal fun getAppSetting(packageManager: PackageManager): AppSetting?
    {
        if (mAppSetting == null) {
            mAppSetting = mSettingsManager.getOrInit(mInfo.packageName, this.getLabel(packageManager))
        }

        return mAppSetting
    }

    internal fun getLabel(packageManager: PackageManager): String
    {
        if (mLabel == null) {
            mLabel = mInfo.loadLabel(packageManager)?.toString() ?: mInfo.packageName
        }

        return mLabel ?: ""
    }

    internal fun getIcon(context: Context, packageManager: PackageManager): Drawable?
    {
        if (mIcon == null) {
            mIcon = mInfo.loadIcon(packageManager)

            if (mIcon == null) {
                mIcon = ResourcesCompat.getDrawable(context.resources, android.R.drawable.sym_def_app_icon, null)
            }
        }

        return mIcon
    }
}
