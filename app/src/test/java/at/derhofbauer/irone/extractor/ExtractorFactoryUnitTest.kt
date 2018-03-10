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

package at.derhofbauer.irone.extractor

import org.junit.Test

import at.derhofbauer.irone.notification.extractor.ExtractorFactory

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class ExtractorFactoryUnitTest
{
    private val extractors: Map<String, Class<*>>
        @Throws(Exception::class)
        get()
        {
            val sExtractors = ExtractorFactory::class.java.getDeclaredField("sExtractors")
            sExtractors.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            return sExtractors.get(null) as Map<String, Class<*>>
        }

    @Test
    @Throws(Exception::class)
    fun getExtractor_correctInstance()
    {
        for ((pkg, cls) in extractors)
        {

            val extractor = ExtractorFactory.getExtractor(pkg)
            assertTrue(cls.isInstance(extractor))
        }
    }

    @Test
    @Throws(Exception::class)
    fun getExtractor_unknown()
    {
        val extractor = ExtractorFactory.getExtractor("unknown.package.name")

        assertNull(extractor)
    }
}
