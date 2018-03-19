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

package at.derhofbauer.irone.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.content.LocalBroadcastManager
import at.derhofbauer.irone.log.Log

import at.derhofbauer.irone.settings.AppSettingsManager
import at.derhofbauer.irone.settings.IroneSettingsManager
import android.content.Intent
import at.derhofbauer.irone.bluetooth.BluetoothService
import at.derhofbauer.irone.calendar.CalendarService


open class NotificationListener : NotificationListenerService()
{
    companion object {
        private const val TAG = "NotificationListener"

        const val BROADCAST_ACTION                = "at.derhofbauer.irone.notification.listener"
        const val ACTION_SEND_TEST_NOTIFICATION   = "send_test_notification"
        const val ACTION_RESULT_TEST_NOTIFICATION = "result_test_notification"
    }

    private var prefIsEnabled = false
    private var mBluetoothService: BluetoothService? = null
    private var mService: NotificationService? = null

    private lateinit var settingsManager: IroneSettingsManager


    private val onChangeEnabled = { value: Boolean ->
        if (value) {
            prefIsEnabled = true
        } else {
            prefIsEnabled = false
            Log.d(TAG, "disabled")
            stopBtService()
        }
    }

    private val onChangeNotifier = { value: String ->
        updateNotificationHandler(value.toInt())
    }


    /**
     * Initializes or returns the notification handler service based on the current setting.
     */
    private fun getNotificationHandler(type: Int): NotificationHandler {
        val context = applicationContext

        return try {
            when (type) {
                IroneSettingsManager.NOTIFIER_BLUETOOTH -> {
                    var service = mBluetoothService
                    if (service == null) {
                       service = BluetoothService(context)
                    }
                    mBluetoothService = service
                    service
                }
                else -> {
                    stopBtService()
                    CalendarService.getInstance(context.contentResolver)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Could not initialize BluetoothService, fallback to CalendarService", e)
            CalendarService.getInstance(context.contentResolver)
        }
    }

    /**
     * Updates or service's notification handler if the service exists and notification handling is
     * enabled.
     */
    private fun updateNotificationHandler(type: Int) {
        if (!prefIsEnabled) {
            return
        }

        val service = mService ?: return
        service.setNotificationHandler(getNotificationHandler(type))
    }

    /**
     * Initializes our notification service (if we're enabled)
     */
    private fun getService(): NotificationService? {
        if (!prefIsEnabled) {
            return null
        }

        if (mService == null) {
            Log.d(TAG, "initializing notification service")

            val context = applicationContext
            val method  = settingsManager.notifier
            val handler = getNotificationHandler(method)

            try {
                mService = NotificationService(
                    handler,
                    context.getSystemService(Context.POWER_SERVICE) as PowerManager,
                    settingsManager,
                    AppSettingsManager.getInstance(context),
                    this
                )

            } catch (e: Exception) {
                Log.e(TAG, "could not start bluetooth service, exiting")
                throw e
            }
        }

        return mService
    }

    /**
     * Stops the bluetooth service.
     */
    private fun stopBtService() {
        if (mBluetoothService == null) {
            return
        }

        Log.d(TAG, "stopping service")
        mBluetoothService?.stop()
        mBluetoothService = null
        mService = null
    }


    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getStringExtra("action")

            when (action) {
                ACTION_SEND_TEST_NOTIFICATION -> {
                    Log.d(TAG, "got test notification intent")
                    sendTestNotification()
                }
            }
        }
    }


    private fun sendTestNotification()
    {
        val result = getService()?.addTestNotification()

        Log.d(TAG, "test notification result: $result")

        val intent = Intent(NotificationListener.BROADCAST_ACTION)
        intent.putExtra("action", NotificationListener.ACTION_RESULT_TEST_NOTIFICATION)
        intent.putExtra("result", result)

        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!prefIsEnabled) {
            Log.d(TAG, "onNotificationPosted: disabled")
            return
        }

        getService()?.handleNotification(sbn)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "created")

        settingsManager = IroneSettingsManager.getInstance(applicationContext)
        prefIsEnabled   = settingsManager.enabled

        settingsManager.onChange(IroneSettingsManager.PREF_ENABLED, onChangeEnabled)
        settingsManager.onChange(IroneSettingsManager.PREF_NOTIFIER, onChangeNotifier)

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(mReceiver, IntentFilter(BROADCAST_ACTION))
    }

    override fun onDestroy() {
        Log.d(TAG, "destroyed")

        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(mReceiver)

        stopBtService()

        settingsManager.removeListener(onChangeEnabled)
        settingsManager.removeListener(onChangeNotifier)

        super.onDestroy()
    }
}
