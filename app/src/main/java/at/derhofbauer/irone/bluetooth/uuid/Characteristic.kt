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

package at.derhofbauer.irone.bluetooth.uuid

import java.util.*

internal class Characteristic: BtLeUUID() {
    companion object {
        val ALERT_CONFIGURATION_CONTROL_POINT: UUID = UUID.fromString(BASE_UUID.format("2A44"))
        val UNREAD_ALERT_STATUS: UUID               = UUID.fromString(BASE_UUID.format("2A45"))
        val NEW_ALERT: UUID                         = UUID.fromString(BASE_UUID.format("2A46"))
        val SUPPORTED_NEW_ALERT_CATEGORY: UUID      = UUID.fromString(BASE_UUID.format("2A47"))
        val SUPPORTED_UNREAD_ALERT_CATEGORY: UUID   = UUID.fromString(BASE_UUID.format("2A48"))

        // primary prorietary service for communication (setup, device info, alerts config, etc)
        val DEVICE_COMMUNICATION: UUID = UUID.fromString(PROPRIETARY_UUID.format("00000023"))
    }
}
