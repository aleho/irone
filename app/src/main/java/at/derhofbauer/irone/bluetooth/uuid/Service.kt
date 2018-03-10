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

internal class Service: BtLeUUID() {
    companion object {
        val ALERT_NOTIFICATION: UUID  = UUID.fromString(BASE_UUID.format("1811"))
        val ALERT_CONFIGURATION: UUID = UUID.fromString(PROPRIETARY_UUID.format("00000020"))

        //XXX correct service here
        //val PROPRIETARY_50: UUID = UUID.fromString(PROPRIETARY_UUID.format("10000050"))
    }
}
