package io.github.satoru2727.slotcounter

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.satoru2727.slotcounter.service.CountingService
import io.github.satoru2727.slotcounter.ui.SlotCounterApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container
        get() = (application as SlotCounterApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The service normally starts when a session starts. This also brings it
        // back after a process death / reboot while a session is still open.
        lifecycleScope.launch {
            if (container.repository.getActiveSession() != null) {
                CountingService.start(this@MainActivity)
            }
        }

        setContent {
            SlotCounterApp(container)
        }
    }

    /**
     * While the counting screen is up, the volume keys are ours. Screen-off
     * presses never reach this method; the foreground service's MediaSession
     * handles those instead.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && container.engine.isRunning.value && container.countingVisible) {
            if (event.repeatCount == 0) {
                val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
                container.engine.onVolumeKey(direction)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && container.engine.isRunning.value && container.countingVisible) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
