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

import android.Manifest
import android.annotation.SuppressLint
import android.app.LoaderManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.view.menu.ActionMenuItemView
import android.support.v7.widget.Toolbar
import at.derhofbauer.irone.log.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.Switch

import at.derhofbauer.irone.appslist.AppEntry
import at.derhofbauer.irone.appslist.ListAdapter
import at.derhofbauer.irone.appslist.ListLoader
import at.derhofbauer.irone.notification.NotificationService
import at.derhofbauer.irone.permissions.PermissionsCheck
import at.derhofbauer.irone.settings.IroneSettingsManager
import android.app.ActivityOptions
import android.content.*
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import at.derhofbauer.irone.calendar.CalendarService
import at.derhofbauer.irone.notification.NotificationHandler
import at.derhofbauer.irone.notification.NotificationListener

class MainActivity : AppCompatActivity(),
    LoaderManager.LoaderCallbacks<List<AppEntry>>,
    NavigationView.OnNavigationItemSelectedListener
{
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var mSettingsManager: IroneSettingsManager
    private lateinit var mDrawer: DrawerLayout
    private lateinit var mSwitch: Switch
    private lateinit var mAdapter: ListAdapter


    private val mPermissionsCheckBluetooth: PermissionsCheck = PermissionsCheck(this, arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ))

    private val mPermissionsCheckCalendar: PermissionsCheck = PermissionsCheck(this, arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    ))

    private val mNotificationsEnableClickListener = { _: DialogInterface, which: Int ->
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> openNotificationSetup()
            DialogInterface.BUTTON_NEGATIVE -> {}
            else -> {}
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getStringExtra("action")

            when (action) {
                NotificationListener.ACTION_RESULT_TEST_NOTIFICATION -> {
                    val result = intent.extras.getInt("result")
                    Log.d(TAG, "test notification result: $result")

                    val resultText = when (result) {
                        NotificationHandler.RESULT_SUCCESS -> context.getString(R.string.action_test_notification_successful)
                        NotificationHandler.RESULT_DELAYED -> context.getString(R.string.action_test_notification_delayed)
                        else                               -> context.getString(R.string.action_test_notification_failed)
                    }

                    Toast.makeText(
                        context,
                        context.getString(R.string.action_test_notification_result, resultText),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setupNavigationView()
        setupActionBar()
        setupListView()

        mSettingsManager = IroneSettingsManager.getInstance(this)

        if (BuildConfig.DEBUG) {
            PermissionsCheck(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)).runThread {
                Log.d(TAG, "will write logs to external storage")
            }
        }

        // ask once at startup
        if (!NotificationService.canAccessNotifications(contentResolver, packageName)) {
            askForNotificationsAccess()
        }

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter(NotificationListener.BROADCAST_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(mBroadcastReceiver)
    }


    private fun setupNavigationView() {
        mDrawer = findViewById(R.id.drawer_layout)
        val nav = findViewById<NavigationView>(R.id.nav_view)

        nav.setNavigationItemSelectedListener(this)
    }

    private fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun setupListView() {
        mAdapter = ListAdapter(this)

        val view = findViewById<ListView>(R.id.apps_listview)
        view.adapter = mAdapter

        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)

        val container = menu.findItem(R.id.action_enable_disable).actionView

        mSwitch = container.findViewById<View>(R.id.action_menu_switch) as Switch

        mSwitch.setOnClickListener { _ ->
            if (mSwitch.isChecked) {
                checkPermissionsBySetting {
                    setIroneEnabled(true)
                }
            } else {
                setIroneEnabled(false)
            }
        }

        updateUiStatus(mSettingsManager.enabled)

        return true
    }


    private fun checkPermissionsBySetting(granted: () -> Unit) {
        val notifier = mSettingsManager.notifier
        val permissionsCheck: PermissionsCheck
        val message: Int

        when (notifier) {
            IroneSettingsManager.NOTIFIER_CALENDAR -> {
                permissionsCheck = mPermissionsCheckCalendar
                message = R.string.permissions_needed_calendar

            }
            IroneSettingsManager.NOTIFIER_BLUETOOTH -> {
                permissionsCheck = mPermissionsCheckBluetooth
                message = R.string.permissions_needed_bluetooth

            }
            else -> {
                Log.e(TAG, "invalid notifier method, won't continue")
                return
            }
        }

        permissionsCheck.runThread(granted) {
            showPermissionsNeededAlert(message)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsCheck.handleCallback(requestCode, grantResults)
    }

    private fun askForNotificationsAccess() {
        val builder = AlertDialog.Builder(this)

        builder
            .setMessage(R.string.notifications_intent_question)
            .setPositiveButton(android.R.string.yes, mNotificationsEnableClickListener)
            .setNegativeButton(android.R.string.no, mNotificationsEnableClickListener)
            .show()
    }

    private fun showPermissionsNeededAlert(message: Int) {
        val builder = AlertDialog.Builder(this)

        builder
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mDrawer.closeDrawer(GravityCompat.START)

        when (item.itemId) {
            R.id.nav_notification_access -> openNotificationSetup()
            R.id.nav_settings            -> openSettings()
            R.id.nav_remove_calendars    -> removeCalendars()
            R.id.nav_test_notifications  -> sendTestNotification()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            mDrawer.openDrawer(GravityCompat.START)
            true
        }

        R.id.action_enable_disable -> {
            if (item.isChecked) {
                item.isChecked = false
                setIroneEnabled(false)
            } else {
                checkPermissionsBySetting {
                    item.isChecked = true
                    setIroneEnabled(true)
                }
            }

            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }


    private fun removeCalendars() {
        mPermissionsCheckCalendar.run {
            setIroneEnabled(false)

            Log.d(TAG, "Removing calendars now")
            CalendarService.getInstance(contentResolver).cleanUp()
        }
    }

    private fun openSettings() {
        val intent  = Intent(this, SettingsActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_right, android.R.anim.fade_out)

        startActivity(intent, options.toBundle())
    }

    private fun openNotificationSetup() {
        startActivity(Intent(NotificationService.SETTINGS_INTENT))
    }

    private fun sendTestNotification() {
        // we can't send a test notification without a running listener
        if (!NotificationService.canAccessNotifications(contentResolver, packageName)) {
            askForNotificationsAccess()
            return
        }

        checkPermissionsBySetting {
            val intent = Intent(NotificationListener.BROADCAST_ACTION)
            intent.putExtra("action", NotificationListener.ACTION_SEND_TEST_NOTIFICATION)

            LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(intent)
        }
    }


    private fun setIroneEnabled(enabled: Boolean) {
        mSettingsManager.enabled = enabled
        mSettingsManager.save()

        updateUiStatus(enabled)
    }

    private fun updateUiStatus(enabled: Boolean) {
        val list = findViewById<View>(R.id.apps_listview)
        if (list != null) {
            list.isEnabled = enabled
        }

        mAdapter.setEnabled(enabled)

        try {
            updateEnableSwitch(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update switch", e)
        }

        try {
            updateEnableCheckbox(enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update checkbox items", e)
        }
    }

    private fun updateEnableSwitch(enabled: Boolean) {
        mSwitch.isChecked = enabled
    }

    @SuppressLint("RestrictedApi")
    private fun updateEnableCheckbox(enabled: Boolean) {
        val check = findViewById<View>(R.id.action_enable_disable)
        val item: MenuItem

        if (check !is ActionMenuItemView) {
            return
        }

        item = check.itemData

        if (item == null || item.isChecked == enabled) {
            return
        }

        with(item) {
            title = resources.getString(
                if (enabled)
                    R.string.action_enabled_title
                else
                    R.string.action_disabled_title
            )

            titleCondensed = resources.getString(
                if (enabled)
                    R.string.action_enabled_title_condensed
                else
                    R.string.action_disabled_title_condensed
            )

            setIcon(
                if (enabled)
                    android.R.drawable.checkbox_on_background
                else
                    android.R.drawable.checkbox_off_background
            )
        }
    }


    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<AppEntry>> {
        return ListLoader(this)
    }

    override fun onLoadFinished(loader: Loader<List<AppEntry>>, data: List<AppEntry>) {
        mAdapter.setData(data)
    }

    override fun onLoaderReset(loader: Loader<List<AppEntry>>) {
        mAdapter.setData(null)
    }
}
