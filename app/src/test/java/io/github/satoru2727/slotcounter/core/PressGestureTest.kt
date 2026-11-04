package io.github.satoru2727.slotcounter.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PressGestureTest {

    @Test
    fun `first press counts in its direction`() {
        assertEquals(PressGesture.Action.COUNT_UP, PressGesture().onPress(1, 1_000))
        assertEquals(PressGesture.Action.COUNT_DOWN, PressGesture().onPress(-1, 1_000))
    }

    @Test
    fun `second press inside the window undoes`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        assertEquals(PressGesture.Action.UNDO, gesture.onPress(1, 1_200))
    }

    @Test
    fun `a third press counts again instead of undoing twice`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        assertEquals(PressGesture.Action.UNDO, gesture.onPress(1, 1_200))
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_400))
    }

    @Test
    fun `presses slower than the window both count`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_400))
    }

    @Test
    fun `key auto-repeat is ignored`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        assertEquals(PressGesture.Action.IGNORE, gesture.onPress(1, 1_040))
        assertEquals(PressGesture.Action.IGNORE, gesture.onPress(1, 1_080))
        assertEquals(PressGesture.Action.IGNORE, gesture.onPress(1, 1_120))
    }

    @Test
    fun `presses on different keys never undo each other`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        assertEquals(PressGesture.Action.COUNT_DOWN, gesture.onPress(-1, 1_200))
    }

    @Test
    fun `release direction is ignored`() {
        assertEquals(PressGesture.Action.IGNORE, PressGesture().onPress(0, 1_000))
    }

    @Test
    fun `reset makes the next press a fresh count`() {
        val gesture = PressGesture()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_000))
        gesture.reset()
        assertEquals(PressGesture.Action.COUNT_UP, gesture.onPress(1, 1_100))
    }
}
