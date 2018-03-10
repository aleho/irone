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

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import at.derhofbauer.irone.bluetooth.uuid.Descriptor
import at.derhofbauer.irone.log.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

internal class Writer: Communication() {
    companion object {
        private const val TAG = "Writer"

        const val DATA_MAX_LEN = 20
    }

    private val charaWritePending: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()
    private val descrWritePending: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()

    var gatt: BluetoothGatt? = null


    override fun getLogTag(): String {
        return TAG
    }


    /**
     * Writes a short array.
     */
    fun write(
        chara: BluetoothGattCharacteristic,
        data: ShortArray,
        onDone: ((Boolean) -> Unit)?
    ) {
        val list = ArrayList<Byte>()

        for (short in data) {
            list.add(short.toByte())
        }

        write(chara, list.toByteArray(), onDone)
    }

    /**
     * Writes a byte array and waits until all chunks were sent, then executes the callback.
     */
    fun write(
        chara: BluetoothGattCharacteristic,
        data: ByteArray,
        onDone: ((Boolean) -> Unit)?
    ) {
        Timer().schedule(timerTask {
            doWriteCharacteristic(chara, data, onDone)
        }, 0)
    }

    /**
     * Enables notifications on a characteristic.
     */
    fun enableNotifications(chara: BluetoothGattCharacteristic, onDone: ((Boolean) -> Unit)?) {
        Timer().schedule(timerTask {
            doEnableNotifications(chara, onDone)
        }, 0)
    }


    /**
     * Writes data in chunks, if necessary.
     * Waits until all chunks were sent, then executes the callback.
     * This blocks until timeout and never successfully finishes multi-parts if not run in a thread.
     */
    private fun doWriteCharacteristic(
        chara: BluetoothGattCharacteristic,
        data: ByteArray,
        onDone: ((Boolean) -> Unit)?
    ) {
        Log.d(TAG, "writing ${chara.uuid}")
        debug(data)

        if (this.gatt == null) {
            Log.w(TAG, "GATT not available, can't write characteristic")
            onDone?.invoke(false)
            return
        }

        if (data.size <= DATA_MAX_LEN) {
            Log.d(TAG, "writing entire array")

            chara.value = data
            val success = writeBlocking(chara)
            onDone?.invoke(success)

            return
        }

        var success = true
        val last = data.size - 1

        for (start in 0..last step DATA_MAX_LEN) {
            val end = minOf(start + DATA_MAX_LEN - 1, last)

            Log.d(TAG, "writing chunk $start, $end")
            chara.value = data.sliceArray(start..end)

            val writeCompleted = writeBlocking(chara)
            if (!writeCompleted) {
                Log.e(TAG, "chunk not written, stopping write")
                success = false
                break
            }
        }

        onDone?.invoke(success)
    }

    /**
     * Enables notifications on a characteristic and waits for the confirmation callback.
     * This blocks until timeout and never finishes successfully if not run in a thread.
     */
    private fun doEnableNotifications(chara: BluetoothGattCharacteristic, onDone: ((Boolean) -> Unit)?) {
        val gatt = this.gatt

        if (gatt == null) {
            Log.w(TAG, "GATT not available, can't enable notifications on ${chara.uuid}")
            onDone?.invoke(false)
            return
        }

        val descriptor = chara.getDescriptor(Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION)
        if (descriptor == null) {
            Log.e(TAG, "could not get client characteristic descriptor ${Descriptor.CLIENT_CHARACTERISTIC_CONFIGURATION}")
            onDone?.invoke(false)
            return
        }

        gatt.setCharacteristicNotification(chara, true)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        val success = writeBlocking(descriptor)

        if (!success) {
            Log.e(TAG, "could not enable notifications on ${chara.uuid}")
        }

        onDone?.invoke(success)
    }


    /**
     * Blocking write of characteristic.
     */
    private inline fun <reified T> writeBlocking(obj: T): Boolean {
        val gatt = this.gatt

        if (gatt == null) {
            // TODO: use our own GATT and add isClosed() for checks
            Log.e(TAG, "GATT closed before writing")
            return false
        }

        setWritePending(obj, true)

        if (obj is BluetoothGattCharacteristic) {
            gatt.writeCharacteristic(obj)
        } else if (obj is BluetoothGattDescriptor) {
            gatt.writeDescriptor(obj)
        }

        val writeCompleted = waitForPendingWrite(obj)

        if (!writeCompleted) {
            return false
        }

        return true
    }

    /**
     * Waits for a pending write on either a characteristic or descriptor.
     */
    private inline fun <reified T> waitForPendingWrite(obj: T): Boolean {
        if (obj !is BluetoothGattCharacteristic && obj !is BluetoothGattDescriptor) {
            Log.e(TAG, "wrong type passed")
            return false
        }

        var writeCompleted = false

        val steps = 1000
        val wait  = 10L

        // wait for 10 seconds at most
        for (j in 1..steps) {
            if (!isWritePending(obj)) {
                writeCompleted = true
                break
            }
            try {
                Thread.sleep(wait)
            } catch (e: InterruptedException) {
                Log.w(TAG, "thread interrupted", e)
            }
        }

        if (!writeCompleted) {
            Log.e(TAG, "write was not completed within ${steps * wait / 1000}s")
            setWritePending(obj, false)
        }

        return writeCompleted
    }


    private inline fun <reified T> setWritePending(obj: T, pending: Boolean) {
        val map: Map<String, Boolean>
        val uuid: String

        if (obj is BluetoothGattCharacteristic) {
            uuid = (obj as BluetoothGattCharacteristic).uuid.toString()
            map  = charaWritePending
        } else {
            uuid = (obj as BluetoothGattDescriptor).uuid.toString()
            map  = descrWritePending
        }

        if (pending) {
            map[uuid] = true
        } else {
            map.remove(uuid)
        }
    }

    private inline fun <reified T> isWritePending(obj: T): Boolean {
        return if (obj is BluetoothGattCharacteristic) {
            val uuid = (obj as BluetoothGattCharacteristic).uuid.toString()
            charaWritePending.containsKey(uuid)

        } else {
            val uuid = (obj as BluetoothGattDescriptor).uuid.toString()
            descrWritePending.containsKey(uuid)
        }
    }


    fun onWriteDone(chara: BluetoothGattCharacteristic) {
        setWritePending(chara, false)
    }

    fun onWriteDone(descr: BluetoothGattDescriptor) {
        setWritePending(descr, false)
    }
}
