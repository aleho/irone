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

package at.derhofbauer.irone.permissions

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.SparseArray

class PermissionsCheck(activity: Activity, permissions: Array<String>)
{
    companion object {
        private var UUID = 1
        private val PERMISSIONS = HashSet<String>()
        private val CALLBACKS   = SparseArray<Callbacks>()

        fun handleCallback(requestCode: Int, grantResults: IntArray) {
            val callbacks = CALLBACKS.get(requestCode) ?: return
            CALLBACKS.remove(requestCode)

            for (result in grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    callbacks.rejected?.invoke()
                    return
                }
            }

            callbacks.granted?.invoke()
        }
    }

    class Callbacks(
        val granted: (() -> Unit)?,
        val rejected: (() -> Unit)?
    )

    private var mPermissions: Array<String> = permissions
    private var mActivity: Activity = activity


    private fun exec(granted: () -> Unit, rejected: (() -> Unit)?) {
        val callbacks = Callbacks(granted, rejected)
        var hasAll    = false

        if (!PERMISSIONS.containsAll(mPermissions.toList())) {
            hasAll = mPermissions.none {
                ContextCompat.checkSelfPermission(mActivity, it) != PackageManager.PERMISSION_GRANTED
            }
        }

        if (hasAll) {
            callbacks.granted?.invoke()
            return
        }

        val uuid = ++UUID
        CALLBACKS.put(uuid, callbacks)
        ActivityCompat.requestPermissions(mActivity, mPermissions, uuid)
    }

    fun run(granted: () -> Unit, rejected: () -> Unit) {
        exec(granted, rejected)
    }

    fun run(granted: () -> Unit) {
        exec(granted, null)
    }

    fun runThread(granted: () -> Unit, rejected: () -> Unit) {
        Handler().post({
            exec(granted, rejected)
        })
    }

    fun runThread(granted: () -> Unit) {
        Handler().post({
            exec(granted, null)
        })
    }
}
