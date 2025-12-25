package com.example.iching.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CastingTest {
    @Test
    fun changedValueMapping() {
        assertEquals(7, changedValue(6))
        assertEquals(8, changedValue(9))
        assertEquals(7, changedValue(7))
        assertEquals(8, changedValue(8))
    }

    @Test
    fun toKeyFromBits() {
        val bits = intArrayOf(1, 0, 1, 0, 1, 0)
        assertEquals(21, toKey(bits))
        val allOnes = intArrayOf(1, 1, 1, 1, 1, 1)
        assertEquals(63, toKey(allOnes))
    }

    @Test
    fun determinismForSeed() {
        val seeds = listOf(1L, 42L, 987654321L)
        for (seed in seeds) {
            val first = castCoins3(seed)
            val second = castCoins3(seed)
            assertArrayEquals(first, second)
        }
    }

    @Test
    fun detectsChangingLines() {
        val lines = intArrayOf(6, 7, 8, 9, 7, 6)
        val expected = listOf(0, 3, 5)
        assertEquals(expected, changingLines(lines))
    }
}
