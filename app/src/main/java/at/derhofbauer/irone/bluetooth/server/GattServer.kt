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

package at.derhofbauer.irone.bluetooth.server

import android.bluetooth.*
import android.content.Context
import at.derhofbauer.irone.log.Log
import at.derhofbauer.irone.bluetooth.comm.Data
import at.derhofbauer.irone.bluetooth.comm.Writer
import at.derhofbauer.irone.bluetooth.uuid.Characteristic
import java.nio.charset.Charset
import kotlin.collections.ArrayList

//TODO: this stopped being a GATT server, rename, and move actual stuff to GATT Server
internal class GattServer(
    private val applicationContext: Context,
    private val manager: BluetoothManager
) {
    companion object {
        private const val TAG = "GattServer"

        // maximum length of once write minus two control bytes
        const val NEW_ALERT_MAX_LEN = Writer.DATA_MAX_LEN - 2
    }

    private val mServiceAlerts: BluetoothGattService = AlertsService.build()

    private var mServer: BluetoothGattServer? = null
    private var mDevice: BluetoothDevice? = null


    /**
     * Starts the GATT server
     */
    fun start(onStarted: (() -> Unit)?) {
        if (mServer != null) {
            Log.d(TAG, "server already started")
            return
        }

        Log.d(TAG, "starting server")
        val server = manager.openGattServer(applicationContext, ServerCallback(
            {
                Log.d(TAG, "server started, device: ${it.address}")
                mDevice = it

                if (onStarted != null) {
                    onStarted()
                }
            },
            this::closeConnections
        ))

        server.addService(mServiceAlerts)

        mServer = server
    }

    /**
     * Stops the GATT server.
     */
    fun stop() {
        closeConnections()
    }


    fun sendAlert(text: String): Boolean {
        val data = buildNotificationData(Data.TYPE_MESSAGE, text)

        return notifyNewAlert(data)
    }

    /**
     * Disconnects the device and closes the GATT server.
     */
    private fun closeConnections() {
        val server = mServer
        val device = mDevice

        mDevice = null
        mServer = null

        if (server != null && device != null) {
            Log.d(TAG, "server.cancelConnection, ${device.address}")
            server.cancelConnection(device)
        }

        if (server != null) {
            Log.d(TAG, "server.close")
            server.close()
        }
    }


    private fun notifyNewAlert(data: ByteArray): Boolean {
        val device = mDevice
        val server = mServer

        if (device == null || server == null) {
            Log.d(TAG, "can't send notification")
            return false
        }

        //TODO: move me to server implementation (complete with mDevice holding, service instantiation, etc)
        val characteristic = mServiceAlerts.getCharacteristic(Characteristic.NEW_ALERT)
        characteristic.value = data

        try {
            val result = server.notifyCharacteristicChanged(device, characteristic, false)
            Log.d(TAG, "notification result: $result")

            return result

        } catch (e: Exception) {
            Log.e(TAG, "could not notify characteristic change", e)
        }

        return false
    }

    /**
     * Builds the byte array for a notification text
     */
    private fun buildNotificationData(type: Byte, text: String): ByteArray {
        val data = ArrayList<Byte>(arrayListOf(
            type,
            (if (type == Data.TYPE_CALL_END) Data.FLAG_DISABLED else Data.FLAG_ENABLED)
        ))

        val bytes = text.toByteArray(Charset.forName("UTF-8"))

        // make sure we never send more than allowed (or available) bytes
        for (i in 0..(minOf(bytes.size, NEW_ALERT_MAX_LEN) - 1)) {
            data.add(bytes[i])
        }

        return data.toByteArray()
    }
}
