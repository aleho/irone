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

package at.derhofbauer.irone

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.*
import android.support.v4.app.NavUtils
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import at.derhofbauer.irone.compat.AppCompatPreferenceActivity
import at.derhofbauer.irone.log.Log
import at.derhofbauer.irone.settings.IroneSettingsManager
import android.preference.ListPreference



class SettingsActivity : AppCompatPreferenceActivity()
{
    companion object {
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and
                Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        setupActionBar()
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }


    override fun onMenuItemSelected(featureId: Int, item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            NavUtils.navigateUpFromSameTask(this)
            animateToMain()
            true
        }

        else -> {
            super.onMenuItemSelected(featureId, item)
        }
    }

    override fun finish() {
        super.finish()

        animateToMain()
    }

    private fun animateToMain() {
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_out_left)
    }


    override fun isValidFragment(fragmentName: String): Boolean {
        return fragmentName == NotificationsPreferences::class.java.name
    }


    class NotificationsPreferences : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener
    {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preferences_notifications)
            setHasOptionsMenu(true)

            onSharedPreferenceChanged(null, IroneSettingsManager.PREF_NOTIFIER)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                IroneSettingsManager.PREF_NOTIFIER -> {
                    val pref = findPreference(key) as ListPreference
                    pref.summary = when (pref.value.toInt()) {
                        IroneSettingsManager.NOTIFIER_CALENDAR -> getString(R.string.pref_notifier_calendar)
                        else                                   -> getString(R.string.pref_notifier_bluetooth)
                    }
                }
            }
        }


        override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(activity, SettingsActivity::class.java))
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}
