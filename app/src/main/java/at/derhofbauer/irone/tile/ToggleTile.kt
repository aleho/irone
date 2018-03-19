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

package at.derhofbauer.irone.tile

import android.annotation.TargetApi
import android.content.ComponentName
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import at.derhofbauer.irone.log.Log
import at.derhofbauer.irone.settings.IroneSettingsManager

@TargetApi(24)
class ToggleTile : TileService() {
    companion object {
        private const val TAG = "Tile"
    }

    private var isEnabled = false
    private var isMonitoring = false

    private var settings: IroneSettingsManager? = null
        get() {
            if (field == null) {
                field = IroneSettingsManager.getInstance(applicationContext)
            }

            return field
        }
        set(value) {
            if (value == null) {
                monitorEnabledChange(false)
            }

            field = value
        }

    /**
     * Callback for settings change.
     */
    private var onChangeEnabled = { value: Boolean ->
        Log.d(TAG, "onChangeEnabled")
        if (value != isEnabled) {
            updateStatusActive(value)
        }
    }

    /**
     * Active tile status update.
     */
    private fun updateStatusActive(enabled: Boolean) {
        Log.d(TAG, "updateStatusActive: $enabled")
        isEnabled = enabled

        val context = applicationContext
        TileService.requestListeningState(context, ComponentName(context, ToggleTile::class.java))
        updateTileState()
    }

    /**
     * Update tile to reflect current state.
     */
    private fun updateTileState() {
        val tile = this.qsTile ?: return
        val state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        if (state != tile.state) {
            tile.state = state
            tile.updateTile()
        }
    }

    /**
     * Monitor changes in enabled setting.
     */
    private fun monitorEnabledChange(monitor: Boolean) {
        if (monitor == isMonitoring) {
            return
        }

        if (monitor) {
            settings?.onChangeImmediate(IroneSettingsManager.PREF_ENABLED, onChangeEnabled)
            isMonitoring = true
        } else {
            settings?.removeListener(onChangeEnabled)
            isMonitoring = false
        }
    }


    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.d(TAG, "removed")
        settings = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "destroyed")
        settings = null
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateStatusActive(settings?.enabled ?: false)
        Log.d(TAG, "added, enabled: $isEnabled")
    }

    override fun onCreate() {
        super.onCreate()
        updateStatusActive(settings?.enabled ?: false)
        Log.d(TAG, "created, enabled: $isEnabled")
        monitorEnabledChange(true)
    }

    override fun onClick() {
        super.onClick()

        isEnabled = !isEnabled
        Log.d(TAG, "clicked, enabled: $isEnabled")
        updateTileState()

        settings?.apply {
            enabled = isEnabled
            save()
        }
    }
}
