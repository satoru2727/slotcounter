package io.github.satoru2727.slotcounter

import android.app.Application
import io.github.satoru2727.slotcounter.ui.AppContainer

class SlotCounterApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
