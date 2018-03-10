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

package at.derhofbauer.irone.notification

import android.app.Notification
import android.os.PowerManager
import android.service.notification.NotificationListenerService

import org.junit.Test

import at.derhofbauer.irone.MockStatusBarNotification
import at.derhofbauer.irone.bluetooth.BluetoothService
import at.derhofbauer.irone.settings.AppSetting
import at.derhofbauer.irone.settings.AppSettingsManager
import at.derhofbauer.irone.settings.IroneSettingsManager
import com.nhaarman.mockito_kotlin.*

class NotificationServiceUnitTest {
    private lateinit var bluetoothService: BluetoothService
    private lateinit var powerManager: PowerManager
    private lateinit var ironeSettings: IroneSettingsManager
    private lateinit var appSettings: AppSettingsManager
    private lateinit var notificationsListener: NotificationListener

    private fun getMockService(isInteractive: Boolean, interruptionFilter: Int): NotificationService {
        bluetoothService = mock()
        powerManager = mock()
        ironeSettings = mock()
        appSettings = mock()
        notificationsListener = mock()

        whenever(powerManager.isInteractive).doReturn(isInteractive)
        whenever(notificationsListener.currentInterruptionFilter).doReturn(interruptionFilter)

        whenever(ironeSettings.onChangeImmediate(any(), any())).doAnswer {
            // always return default setting as true
            (it.getArgument(1) as (Any) -> Unit).invoke(true)
        }

        val service = NotificationService(bluetoothService, powerManager, ironeSettings, appSettings, notificationsListener)

        return service
    }

    @Test
    fun handlePreferences() {
        // TODO: Test different prefs
        // TODO: test bogus id
        // TODO: test groups
    }

    @Test
    fun handleNotification_interruption_filtered() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALARMS
        )

        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            "some.unknown.package",
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        service.handleNotification(sbn)

        verify(notificationsListener, atLeast(1)).currentInterruptionFilter
        verify(notificationsListener, atMost(2)).currentInterruptionFilter
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }

    @Test
    fun handleNotification_calendar_notification_ignored() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            "com.android.calendar",
            Notification.CATEGORY_EVENT,
            title,
            text,
            "",
            ""
        )

        service.handleNotification(sbn)

        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(appSettings)
    }

    @Test
    fun handleNotification_device_interactive() {
        val service = getMockService(
            true,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            "some.unknown.package",
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        service.handleNotification(sbn)

        verify(notificationsListener, times(1)).currentInterruptionFilter
        verify(powerManager, times(1)).isInteractive
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }


    @Test
    fun handleNotification_unknown_setting() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val packageName = "some.unknown.package"
        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        service.handleNotification(sbn)

        verify(notificationsListener, times(1)).currentInterruptionFilter
        verify(powerManager, times(1)).isInteractive
        verify(appSettings, times(1))[packageName]
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }


    @Test
    fun handleNotification_disabled_setting() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val packageName = "some.known.package"
        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        val setting = mock<AppSetting>()
        whenever(setting.isEnabled).doReturn(false)
        whenever(appSettings[packageName]).doReturn(setting)

        service.handleNotification(sbn)

        verify(notificationsListener, times(1)).currentInterruptionFilter
        verify(powerManager, times(1)).isInteractive
        verify(appSettings, times(1))[packageName]
        verify(setting, times(1)).isEnabled
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }

    @Test
    fun handleNotification_event_added() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val packageName = "some.known.package"
        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        val setting = mock<AppSetting>()
        whenever(setting.isEnabled).doReturn(true)
        whenever(appSettings[packageName]).doReturn(setting)

        sbn.data.groupKey = "my-group-key"
        service.handleNotification(sbn)

        verify(notificationsListener, times(1)).currentInterruptionFilter
        verify(powerManager, times(1)).isInteractive
        verify(appSettings, times(1))[packageName]
        verify(setting, times(1)).isEnabled
        verify(bluetoothService, times(1)).addNotification(isA())
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)


        // do not add another event because of group key
        service.handleNotification(sbn)

        verify(notificationsListener, times(2)).currentInterruptionFilter
        verify(powerManager, times(2)).isInteractive
        verify(appSettings, times(2))[packageName]
        verify(setting, times(2)).isEnabled

        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }

    @Test
    fun handleNotification_skipped() {
        val service = getMockService(
            false,
            NotificationListenerService.INTERRUPTION_FILTER_ALL
        )

        val packageName = "some.known.package"
        val text = "Text"
        val title = "Title"

        val sbn = MockStatusBarNotification.build(
            packageName,
            Notification.CATEGORY_MESSAGE,
            title,
            text,
            "",
            ""
        )

        sbn.data.isOngoing = true
        service.handleNotification(sbn)

        // notification is ongoing, must skip
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)


        sbn.data.isOngoing = false
        sbn.data.packageName = "android"
        service.handleNotification(sbn)

        // system package name, must skip
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)


        sbn.data.groupKey = "my-group-key"
        sbn.data.packageName = "not.skipped.package"

        service.handleNotification(sbn)

        // group key, will not skip, but has no app setting
        verify(notificationsListener, times(1)).currentInterruptionFilter
        verify(powerManager, times(1)).isInteractive
        verify(appSettings, times(1))["not.skipped.package"]
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)

        service.handleNotification(sbn)

        // group key found, first needs call to app settings, must not call calendar service (because 0)
        verify(notificationsListener, times(2)).currentInterruptionFilter
        verify(powerManager, times(2)).isInteractive
        verify(appSettings, times(2))["not.skipped.package"]
        verifyNoMoreInteractions(bluetoothService)
        verifyNoMoreInteractions(powerManager)
        verifyNoMoreInteractions(notificationsListener)
        verifyNoMoreInteractions(appSettings)
    }
}
