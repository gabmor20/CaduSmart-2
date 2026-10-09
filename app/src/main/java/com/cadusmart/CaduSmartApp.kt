package com.cadusmart

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cadusmart.work.ExpiryNotifier
import com.cadusmart.work.ExpiryScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CaduSmartApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()   // Hilt inyecta aquí; debe ir antes de usar WorkManager
        ExpiryNotifier.createChannel(this)
        ExpiryScheduler.schedule(this)
    }
}
