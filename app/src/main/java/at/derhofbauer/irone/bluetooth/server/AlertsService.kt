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

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import at.derhofbauer.irone.bluetooth.comm.Data
import at.derhofbauer.irone.bluetooth.uuid.Characteristic
import at.derhofbauer.irone.bluetooth.uuid.Descriptor
import at.derhofbauer.irone.bluetooth.uuid.Service

internal class AlertsService: AbstractService()
{
    companion object {
        fun build(): BluetoothGattService {
            val service = BluetoothGattService(Service.ALERT_NOTIFICATION, BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val alertConfigPoint = BluetoothGattCharacteristic(
                Characteristic.ALERT_CONFIGURATION_CONTROL_POINT,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            // new alert notification
            val newAlert = BluetoothGattCharacteristic(
                Characteristic.NEW_ALERT,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                0
            )
            newAlert.addDescriptor(BluetoothGattDescriptor(
                Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            ))

            // unread alert notification
            val unreadAlertStatus = BluetoothGattCharacteristic(
                Characteristic.UNREAD_ALERT_STATUS,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                0
            )
            unreadAlertStatus.addDescriptor(BluetoothGattDescriptor(
                Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            ))

            // supported alerts
            val newAlertCategory = BluetoothGattCharacteristic(
                Characteristic.SUPPORTED_NEW_ALERT_CATEGORY,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            newAlertCategory.setValue(toBitField(Data.SUPPORTED_ALERTS), BluetoothGattCharacteristic.FORMAT_UINT16, 0)

            val unreadAlertCategory = BluetoothGattCharacteristic(
                Characteristic.SUPPORTED_UNREAD_ALERT_CATEGORY,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            unreadAlertCategory.setValue(toBitField(Data.SUPPORTED_ALERTS), BluetoothGattCharacteristic.FORMAT_UINT16, 0)

            service.addCharacteristic(alertConfigPoint)
            service.addCharacteristic(newAlert)
            service.addCharacteristic(unreadAlertStatus)
            service.addCharacteristic(newAlertCategory)
            service.addCharacteristic(unreadAlertCategory)

            return service
        }
    }
}
