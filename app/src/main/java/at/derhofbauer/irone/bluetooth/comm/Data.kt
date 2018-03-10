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

package at.derhofbauer.irone.bluetooth.comm

import android.bluetooth.BluetoothDevice
import android.os.Build
import java.util.*

/**
 * Collection of data used to communicate with the device.
 */
class Data
{
    companion object {
        const val DEVICE_NAME = "Steel HR"

        const val TYPE_CALL_START: Byte = 0x03
        const val TYPE_CALL_END: Byte   = 0x04
        const val TYPE_MESSAGE: Byte    = 0x05
        const val TYPE_EVENT: Byte      = 0x07
        const val FLAG_ENABLED: Byte    = 0x01
        const val FLAG_DISABLED: Byte   = 0x00

        val SUPPORTED_ALERTS: List<Byte> = Arrays.asList(TYPE_CALL_START, TYPE_MESSAGE, TYPE_EVENT, 0)

        val DEVICE_INFO_REQUEST = shortArrayOf(
            0x01, 0x01, 0x01, 0x00, 0x10,       // command: get info, length 16
            0x01, 0x2a, 0x00, 0x06,             // command: get some more info, length 6
            0x01, 0x01, 0x00, 0x2e, 0x8b, 0xa1, // whatever
            0x09, 0x28, 0x00, 0x02, 0x00, 0x19  // last byte is some information string
        )

        /**
         * TODO: real device information checks here! (maybe UUID, mac address, send device probe via connection?)
         */
        fun isOurDevice(device: BluetoothDevice): Boolean {
            if (device.type != BluetoothDevice.DEVICE_TYPE_LE
                || !device.name.startsWith(DEVICE_NAME)
            ) {
                return false
            }

            return true
        }
    }
}
