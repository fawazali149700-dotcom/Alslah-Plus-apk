package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.DhikrRepository
import com.example.data.repository.MuezzinRepository
import com.example.service.AdhanAlarmScheduler
import com.example.service.NotificationHelper
import com.example.worker.WidgetSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoorApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createAllChannels(this)
        WidgetSyncWorker.schedulePeriodicSync(this)

        appScope.launch {
            val db = AppDatabase.getInstance(this@NoorApplication)
            MuezzinRepository(this@NoorApplication, db.muezzinDao()).ensureSeeded()
            DhikrRepository(this@NoorApplication, db.dhikrDao()).ensureCoreVoicesReady()

            val settings = SettingsDataStore(this@NoorApplication).getSettingsOnce()
            AdhanAlarmScheduler(this@NoorApplication).rescheduleAll(settings)
        }
    }
}
