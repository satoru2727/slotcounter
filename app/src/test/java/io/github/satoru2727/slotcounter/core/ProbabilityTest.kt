package io.github.satoru2727.slotcounter.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProbabilityTest {

    @Test
    fun `no hits or no games has no probability`() {
        assertNull(Probability.format(0, 100))
        assertNull(Probability.format(10, 0))
        assertNull(Probability.format(0, 0))
        assertNull(Probability.format(-3, 100))
    }

    @Test
    fun `small odds keep one decimal`() {
        assertEquals("1/6.0", Probability.format(1, 6))
        assertEquals("1/8.1", Probability.format(123, 1_000))
    }

    @Test
    fun `large odds are rounded to an integer`() {
        assertEquals("1/128", Probability.format(1, 128))
        assertEquals("1/100", Probability.format(1, 100))
    }

    @Test
    fun `exact division trims at the display scale`() {
        assertEquals("1/50.0", Probability.format(2, 100))
    }
}
