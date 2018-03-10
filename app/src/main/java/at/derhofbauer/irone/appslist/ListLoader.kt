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

import android.content.AsyncTaskLoader
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

import java.text.Collator
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.HashSet

import at.derhofbauer.irone.BuildConfig
import at.derhofbauer.irone.settings.AppSettingsManager

class ListLoader(context: Context) : AsyncTaskLoader<List<AppEntry>>(context)
{
    companion object
    {
        private val BLACKLIST = HashSet(
            Arrays.asList(
                BuildConfig.APPLICATION_ID,
                "com.android.calendar",
                "com.android.dialer",
                "com.android.settings",
                "com.google.android.calendar",
                "com.google.android.dialer",
                "com.withings.wiscale2"
            )
        )
    }

    private val compLabelAlphabetically = object : Comparator<AppEntry> {
        private val sCollator = Collator.getInstance()
        private val packageManager = getContext().packageManager

        override fun compare(object1: AppEntry, object2: AppEntry): Int {
            return sCollator.compare(object1.getLabel(packageManager), object2.getLabel(packageManager))
        }
    }

    private var mAppsList: List<AppEntry>? = null

    override fun loadInBackground(): List<AppEntry>
    {
        val context = context
        val mPackageManager = context.packageManager
        val settings = AppSettingsManager.getInstance(context)
        var apps: List<ApplicationInfo>? = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        if (apps == null) {
            apps = ArrayList()
        }

        val entries = ArrayList<AppEntry>()

        for (app in apps) {
            if (BLACKLIST.contains(app.packageName)) {
                continue
            }

            // only actually "startable" apps
            mPackageManager.getLaunchIntentForPackage(app.packageName) ?: continue

            entries.add(AppEntry(settings, app))
        }

        Collections.sort(entries, compLabelAlphabetically)

        return entries
    }

    override fun deliverResult(apps: List<AppEntry>?)
    {
        if (isReset) {
            if (apps != null) {
                onReleaseResources(apps)
            }
        }

        val oldApps = mAppsList
        mAppsList = apps

        if (isStarted) {
            super.deliverResult(apps)
        }

        if (oldApps != null) {
            onReleaseResources(oldApps)
        }
    }

    override fun onStartLoading()
    {
        if (mAppsList != null) {
            deliverResult(mAppsList)
        }

        if (takeContentChanged() || mAppsList == null) {
            forceLoad()
        }
    }

    override fun onStopLoading()
    {
        cancelLoad()
    }

    override fun onCanceled(apps: List<AppEntry>)
    {
        super.onCanceled(apps)

        onReleaseResources(apps)
    }

    override fun onReset()
    {
        super.onReset()

        onStopLoading()

        if (mAppsList != null) {
            onReleaseResources(mAppsList)
            mAppsList = null
        }
    }

    private fun onReleaseResources(apps: List<AppEntry>?)
    {
        //nothing to release
    }
}
