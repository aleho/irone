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

package at.derhofbauer.irone

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.nhaarman.mockito_kotlin.*

class MockStatusBarNotification private constructor() : StatusBarNotification(null)
{
    lateinit var data: Holder

    class Holder
    {
        var id: Int = 0
        var packageName: String? = null
        var category: String? = null
        var title: String? = null
        var text: String? = null
        var summary: String? = null
        var subText: String? = null
        var uid: String? = null
        var isOngoing: Boolean = false
        var groupKey: String? = null
        var notification: Notification? = null
    }

    companion object Factory
    {
        private var UID = 0

        fun build(
            pkg: String,
            category: String?,
            title: String?,
            text: String?,
            summary: String?,
            subText: String?
        ): MockStatusBarNotification
        {
            val holder = Holder()
            holder.uid = "uid-" + (++UID).toString()
            holder.packageName = pkg
            holder.category = category
            holder.title = title
            holder.text = text
            holder.summary = summary
            holder.subText = subText
            holder.notification = getMockNotification(holder)

            val sbn = mock<MockStatusBarNotification>()
            sbn.data = holder

            whenever(sbn.data).doReturn(holder)
            whenever(sbn.id).doAnswer { _ -> holder.id }
            whenever(sbn.key).doAnswer { _ -> holder.packageName + "-" + holder.uid }
            whenever(sbn.notification).doAnswer { _ -> holder.notification }
            whenever(sbn.packageName).doAnswer { _ -> holder.packageName }
            whenever(sbn.isOngoing).doAnswer { _ -> holder.isOngoing }
            whenever(sbn.groupKey).doAnswer { _ -> holder.groupKey }

            return sbn
        }

        private fun getMockNotification(holder: Holder): Notification
        {
            val notification = mock<Notification>()
            val bundle = mock<Bundle>()

            notification.category = holder.category
            notification.extras = bundle

            whenever(bundle.getString(Notification.EXTRA_TITLE))
                .doAnswer { _ -> holder.title }

            whenever(bundle.getCharSequence(Notification.EXTRA_TEXT))
                .doAnswer { _ -> if (holder.text != null) StringBuffer(holder.text) else null }

            whenever(bundle.getString(Notification.EXTRA_SUMMARY_TEXT))
                .doAnswer { _ -> holder.summary }

            whenever(bundle.getString(Notification.EXTRA_SUB_TEXT))
                .doAnswer { _ -> holder.subText }

            whenever(bundle.getString("UID"))
                .doAnswer { _ -> holder.uid }

            return notification
        }
    }
}
