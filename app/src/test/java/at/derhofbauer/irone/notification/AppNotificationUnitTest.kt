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

import org.junit.Assert.assertEquals
import org.junit.Test

internal class AppNotificationUnitTest
{
    @Test
    fun test_stringUnicode() {
        val an = AppNotification()

        an.from = " From"
        // strings contain a peach emoji between underscores!
        an.primary = " _🍑_"
        an.secondary = " _🍑_"

        assertEquals(5, an.primary!!.length)
        assertEquals(5, an.secondary!!.length)

        val strings = an.string
        assertEquals("From: __ __", strings)
    }

    @Test
    fun test_stringUnicodeEscaped() {
        val an = AppNotification()

        an.from = " From"
        an.primary = " _\uD83C\uDF51_ "
        an.secondary = " _\uD83C\uDF51_ "

        assertEquals(6, an.primary!!.length)
        assertEquals(6, an.secondary!!.length)

        val strings = an.string
        assertEquals("From: __ __", strings)
    }
}
