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

import android.bluetooth.*
import at.derhofbauer.irone.BuildConfig
import at.derhofbauer.irone.bluetooth.comm.Reader
import at.derhofbauer.irone.bluetooth.comm.Writer
import at.derhofbauer.irone.log.Log

/**
 * Callback for our GATT connection after finding a device.
 */
internal class ConnectionCallback(
    private val onDiscovered: (BluetoothGatt) -> Unit,
    private val writer: Writer,
    private val reader: Reader
) : BluetoothGattCallback()
{
    companion object {
        private const val TAG = "ConnectionCallback"
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (gatt == null) {
            Log.d(TAG, "onServicesDiscovered without gatt: $status")
            return
        }

        val device = gatt.device

        if (device == null) {
            Log.d(TAG, "onServicesDiscovered without device: $status")
            return
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered unsuccessful")
            return
        }

        //debugServices()
        onDiscovered(gatt)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (gatt == null) {
            Log.d(TAG, "state change without gatt: $status $newState")
            return
        }

        val device = gatt.device

        if (device == null) {
            Log.d(TAG, "state change without device: $status $newState")
            return
        }

        //TODO congested!
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
                gatt.discoverServices()
            }

            BluetoothProfile.STATE_DISCONNECTING -> {
                Log.d(TAG, "disconnecting, device: ${device.address}, ${device.name}")
                //XXX disconnected, let client know
            }

            BluetoothProfile.STATE_DISCONNECTED -> {
                Log.d(TAG, "disconnected, device: ${device.address}, ${device.name}")
            }
        }
    }


    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        Log.d(TAG, "characteristic read: ${characteristic?.uuid}")
    }

    override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        Log.d(TAG, "descriptor read: ${descriptor?.uuid}")
    }


    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        if (characteristic == null) {
            Log.e(TAG, "onCharacteristicWrite without characteristic")
            return
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "characteristic write unsuccessful: ${characteristic.uuid}")
            return
        }

        //XXX need onWriteFailed

        writer.onWriteDone(characteristic)
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        if (characteristic == null) {
            Log.d(TAG, "onCharacteristicChanged without characteristic")
            return
        }

        reader.read(characteristic)
    }


    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        if (descriptor == null) {
            Log.e(TAG, "onDescriptorWrite without descriptor")
            return
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "descriptor write unsuccessful: ${descriptor.uuid}")
            return
        }

        //XXX need onWriteFailed

        writer.onWriteDone(descriptor)
    }


    /**
     * Debug helper for GATT services.
     */
    private fun debugServices(gatt: BluetoothGatt) {
        val device = gatt.device

        if (!BuildConfig.DEBUG || device == null) {
            return
        }

        Log.d(TAG, "services: $device")

        for (service in gatt.services) {
            Log.d(TAG, "service          ${service.uuid}\n")
            Log.d(TAG, " \n")

            for (chara in service.characteristics) {
                Log.d(TAG, "  characteristic ${chara.uuid}")

                for (desc in chara.descriptors) {
                    Log.d(TAG, "    descriptor   ${desc.uuid}")
                }

                Log.d(TAG, " \n")
            }

            Log.d(TAG, " \n")
        }
    }
}
