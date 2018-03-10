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

package at.derhofbauer.irone.bluetooth.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanResult
import android.content.Context
import at.derhofbauer.irone.bluetooth.comm.Data
import at.derhofbauer.irone.bluetooth.comm.Reader
import at.derhofbauer.irone.bluetooth.comm.Writer
import at.derhofbauer.irone.bluetooth.uuid.Characteristic
import at.derhofbauer.irone.bluetooth.uuid.Service
import at.derhofbauer.irone.log.Log

internal class GattClient(
    private val applicationContext: Context,
    private val manager: BluetoothManager
) {
    companion object {
        private const val TAG = "GattClient"
    }

    private val mWriter = Writer()

    private var mScanCallback: LeScanCallback? = null
    private var mGatt: BluetoothGatt? = null
    private var mDevice: BluetoothDevice? = null

    private var mOnConnectedCallback: ((Boolean) -> Unit)? = null


    /**
     * Callback for scan results when searching for devices.
     */
    internal inner class LeScanCallback : android.bluetooth.le.ScanCallback() {
        private fun stopLeScan() {
            val callback = mScanCallback
            mScanCallback = null

            if (callback != null) {
                manager.adapter?.bluetoothLeScanner?.stopScan(callback)
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return

            if (mDevice != null) {
                stopLeScan()
                return
            }

            Log.d(TAG, "onScanResult, ${device.address}, ${device.name}")
            if (Data.isOurDevice(device) && mDevice == null) {
                stopLeScan()
                connect(device)
            }
        }
    }


    /**
     * Starts the GATT client.
     */
    fun start(onDone: (Boolean) -> Unit) {
        if (mDevice != null) {
            Log.d(TAG, "already connected")
            onDone(true)
            return
        }

        Log.d(TAG, "searching for device")
        mOnConnectedCallback = onDone
        searchDevice()
    }

    /**
     * Stops the GATT client.
     */
    fun stop() {
        Log.d(TAG, "stopping")
        closeConnections()
    }

    /**
     * Configures notifications for this connection.
     */
    fun configureNotifications(onDone: ((Boolean) -> Unit)) {
        val gatt = mGatt

        if (gatt == null) {
            Log.w(TAG, "can't configure notifications, not connected")
            onDone(false)
            return
        }

        val service = gatt.getService(Service.ALERT_CONFIGURATION)

        if (service == null) {
            Log.e(TAG, "could not get notifications service")
            onDone(false)
            return
        }

        val chara = service.getCharacteristic(Characteristic.DEVICE_COMMUNICATION)
        if (chara == null) {
            Log.e(TAG, "could not get alerts characteristic ${Characteristic.DEVICE_COMMUNICATION}")
            onDone(false)
            return
        }

        mWriter.enableNotifications(chara) {
            if (!it) {
                Log.d(TAG, "enable notification failed on ${chara.uuid}")
                onDone(false)
                return@enableNotifications
            }

            mWriter.write(chara, Data.DEVICE_INFO_REQUEST) {
                if (!it) {
                    Log.d(TAG, "could not send device info request")
                }

                onDone(it)
            }
        }
    }


    private fun searchDevice() {
        Log.d(TAG, "checking connected devices")

        for (device in manager.getConnectedDevices(BluetoothProfile.GATT)) {
            if (Data.isOurDevice(device) && mDevice == null) {
                connect(device)
                return
            }
        }

        val callback = LeScanCallback()
        mScanCallback = callback

        val scanner = manager.adapter?.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "could not get LE scanner")
            return
        }

        Log.d(TAG, "starting scan")
        scanner.startScan(callback)
    }

    /**
     * Called after scanning for our device.
     */
    private fun connect(device: BluetoothDevice) {
        if (mDevice !== null) {
            Log.d(TAG, "already got a device: ${device.address}, ${device.name}")
            return
        }

        if (!Data.isOurDevice(device)) {
            Log.d(TAG, "unknown device: ${device.address}, ${device.name}")
            return
        }

        mDevice = device

        Log.d(TAG, "found our device: ${device.address}")

        // TODO autoconnect logic
        device.connectGatt(applicationContext, false, ConnectionCallback(
            this::onServicesDiscovered,
            mWriter,
            Reader() //TODO: remove me or implement me
        ))
    }

    /**
     * Called after device services were discovered.
     */
    private fun onServicesDiscovered(gatt: BluetoothGatt) {
        mGatt = gatt
        mWriter.gatt = gatt

        val callback = mOnConnectedCallback
        if (callback != null) {
            mOnConnectedCallback = null
            callback(true)
        }
    }


    /**
     * Disconnects the device and closes the GATT server.
     */
    private fun closeConnections() {
        val device = mDevice
        val gatt   = mGatt

        mDevice = null
        mGatt = null
        mWriter.gatt = null

        if (gatt != null) {
            Log.d(TAG, "gatt.close")
            if (device != null) {
                gatt.disconnect()
            }

            gatt.close()
        }
    }
}
