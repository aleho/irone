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
import at.derhofbauer.irone.log.Log

/**
 * Callback for our GATT server for notifications.
 */
internal class ServerCallback(
    private val onConnected: (device: BluetoothDevice) -> Unit,
    private val onDisconnected: () -> Unit

) : BluetoothGattServerCallback()
{
    companion object {
        private const val TAG = "ServerCallback"
    }

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        if (device == null) {
            Log.d(TAG, "state change without device: $status $newState")
            return
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "unsuccessful status change: $status $newState, device: ${device.address}, ${device.name}")
            return
        }

        when (newState) {
            BluetoothProfile.STATE_CONNECTING -> {
                Log.d(TAG, "connecting, device: ${device.address}, ${device.name}")
            }

            BluetoothProfile.STATE_CONNECTED -> {
                Log.d(TAG, "connected, device: ${device.address}, ${device.name}")
                onConnected(device)
            }

            BluetoothProfile.STATE_DISCONNECTING -> {
                Log.d(TAG, "disconnecting, device: ${device.address}, ${device.name}")
                // just to be sure we're actually disconnecting it
                onDisconnected()
            }

            BluetoothProfile.STATE_DISCONNECTED -> {
                Log.d(TAG, "disconnected, device: ${device.address}, ${device.name}")
                onDisconnected()
            }
        }
    }


    override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
        Log.d(TAG, "characteristic read request $requestId: ${characteristic?.uuid}")
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
        Log.d(TAG, "characteristic write request $requestId: ${characteristic?.uuid}")
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
        Log.d(TAG, "descriptor read request $requestId: ${descriptor?.uuid}")
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
        Log.d(TAG, "descriptor write request $requestId: ${descriptor?.uuid}")
    }

    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
        Log.d(TAG, "write execute request $requestId: $execute")
    }
}
