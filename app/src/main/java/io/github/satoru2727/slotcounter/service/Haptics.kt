package io.github.satoru2727.slotcounter.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptics are the only feedback while the phone is in a pocket: one short tick
 * per count, a double tick for an undo. The vibrator works with the screen off.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var enabled: Boolean = true

    fun tick() = vibrate(longArrayOf(0, 18), intArrayOf(0, 180))

    fun undo() = vibrate(longArrayOf(0, 16, 55, 16), intArrayOf(0, 200, 0, 200))

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        if (!enabled) return
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            device.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}
