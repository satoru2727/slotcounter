package io.github.satoru2727.slotcounter.core

/**
 * Turns a stream of volume-key presses into count/undo actions.
 *
 * The physical keys are the only input while the phone sits in a pocket, so
 * every press is one of three things:
 *
 *  - a single press: count the counter bound to that key
 *  - a second press within [doubleMs]: undo the previous count
 *  - a repeated press while the key is held: ignored (the first press counted)
 *
 * The state is confined to the caller's single consumer (CounterEngine) so it
 * does not need locks. Time is passed in, which keeps this class unit-testable.
 */
class PressGesture(
    private val repeatMs: Long = 70,
    private val doubleMs: Long = 350,
) {
    enum class Action { COUNT_UP, COUNT_DOWN, UNDO, IGNORE }

    private var lastAt: Long = NEVER
    private var lastDirection = 0
    private var lastWasUndo = false

    fun onPress(direction: Int, now: Long): Action {
        if (direction == 0) return Action.IGNORE
        val gap = now - lastAt
        lastAt = now

        // Key auto-repeat while held down; the first press already counted.
        if (gap in 0 until repeatMs) return Action.IGNORE

        // A double click on the same key is an undo, but a third click must
        // count again. Requiring the same key keeps two different counters
        // pressed in quick succession from cancelling each other.
        if (!lastWasUndo && direction == lastDirection && gap in repeatMs until doubleMs) {
            lastWasUndo = true
            return Action.UNDO
        }

        lastWasUndo = false
        lastDirection = direction
        return if (direction > 0) Action.COUNT_UP else Action.COUNT_DOWN
    }

    fun reset() {
        lastAt = NEVER
        lastDirection = 0
        lastWasUndo = false
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE / 4
    }
}
