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

package at.derhofbauer.irone.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import at.derhofbauer.irone.bluetooth.server.GattServer
import at.derhofbauer.irone.log.Log
import at.derhofbauer.irone.notification.AppNotification
import at.derhofbauer.irone.notification.NotificationHandler
import java.util.*
import kotlin.concurrent.timerTask

class BluetoothService(private val applicationContext: Context) : NotificationHandler()
{
    companion object {
        private const val TAG = "BluetoothService"
        // wait a bit for connection to settle
        private const val DELAYED_TIMEOUT = 2000L

        const val TEXT_MAX_LENGTH = GattServer.NEW_ALERT_MAX_LEN
    }


    private val mServer: GattServer
    private val mStateChangeReceiver: StateChangeReceiver
    private val mManager: BluetoothManager

    private var isEnabled = false
    private var hasConnection: Boolean = false

    /**
     * Notification to be send after connecting. This will always be the latest notification to not nag users.
     */
    private var mDelayedNotification: AppNotification? = null


    init {
        // manager required
        val manager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) ?: throw RuntimeException("Could not get bluetooth manager")

        mManager = manager as BluetoothManager
        mServer  = GattServer(applicationContext, manager)

        // adapter required as well
        val adapter = manager.adapter ?: throw RuntimeException("Could not get bluetooth adapter")

        isEnabled = if (adapter.isEnabled) {
            true
        } else {
            Log.d(TAG, "adapter disabled")
            false
        }

        mStateChangeReceiver = StateChangeReceiver()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        applicationContext.registerReceiver(mStateChangeReceiver, filter)
    }


    /**
     * Stopps the service, killing all connections.
     */
    fun stop() {
        close()

        applicationContext.unregisterReceiver(mStateChangeReceiver)
    }


    override fun addNotification(notification: AppNotification): Int {
        if (!isEnabled) {
            Log.d(TAG, "bluetooth disabled, can't notify")

            return RESULT_FAILURE
        }

        if (!hasConnection) {
            mDelayedNotification = notification
            start()

            return RESULT_DELAYED
        }

        mDelayedNotification = null

        return if (mServer.sendAlert(notification.string)) RESULT_SUCCESS else RESULT_FAILURE
    }


    private fun sendDelayedNotification() {
        if (mDelayedNotification !== null) {
            Log.d(TAG, "scheduling delayed send")
            Timer().schedule(timerTask {
                val notification = mDelayedNotification ?: return@timerTask
                addNotification(notification)
            }, DELAYED_TIMEOUT)
        }
    }


    private fun close() {
        hasConnection = false
        closeServer()
    }

    private fun closeServer() {
        Log.d(TAG, "closing server")
        mServer.stop()
    }


    private fun start() {
        mServer.start({
            hasConnection = true
            sendDelayedNotification()
        }, {
            hasConnection = false
        })
    }


    /**
     * Observer for Bluetooth changes.
     */
    private inner class StateChangeReceiver: BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return

            if (action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                return
            }

            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

            when (state) {
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    Log.d(TAG, "bluetooth turning off")
                    isEnabled = false
                    close()
                }

                BluetoothAdapter.STATE_ON -> {
                    Log.d(TAG, "bluetooth turned on")
                    isEnabled = true
                }
            }
        }
    }
}
