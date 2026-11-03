package io.github.davamix.workouttimer

import android.app.Application
import io.github.davamix.workouttimer.di.AppContainer

class WorkoutTimerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
