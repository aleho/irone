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

import android.bluetooth.BluetoothGattCharacteristic

internal class Reader: Communication() {
    companion object {
        private const val TAG = "Reader"
    }

    override fun getLogTag(): String {
        return TAG
    }

    fun read(chara: BluetoothGattCharacteristic) {
        //TODO: add timeout for reading, read into list, build data from it
        debug(chara.value)
    }
}
