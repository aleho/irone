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

package android.util

/**
 * Mock to "override" Android util class.
 * JvmStatic to work in real classes.
 */
object Log
{
    @JvmStatic
    fun d(tag: String, msg: String): Int
    {
        println("DEBUG: $tag: $msg")
        return 0
    }

    @JvmStatic
    fun i(tag: String, msg: String): Int
    {
        println("INFO: $tag: $msg")
        return 0
    }

    @JvmStatic
    fun w(tag: String, msg: String): Int
    {
        println("WARN: $tag: $msg")
        return 0
    }

    @JvmStatic
    fun e(tag: String, msg: String): Int
    {
        println("ERROR: $tag: $msg")
        return 0
    }
}
