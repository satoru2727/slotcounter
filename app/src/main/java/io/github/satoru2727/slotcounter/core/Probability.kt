package io.github.satoru2727.slotcounter.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Probability display for a counter. Everything is recomputed from the raw
 * count and game count, so editing the game count is retroactive by design.
 */
object Probability {

    /** Games per hit, or null when there is nothing to divide by. */
    fun oneIn(count: Int, games: Int): BigDecimal? {
        if (count <= 0 || games <= 0) return null
        return BigDecimal(games).divide(BigDecimal(count), 4, RoundingMode.HALF_UP)
    }

    /**
     * "1/6.4" below 100, "1/128" above. One decimal is what halls print for
     * small-role odds; integers stay exact for the big ones.
     */
    fun format(count: Int, games: Int): String? {
        val value = oneIn(count, games) ?: return null
        val scale = if (value >= BigDecimal(100)) 0 else 1
        return "1/" + value.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}
