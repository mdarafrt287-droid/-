package com.example

import android.app.Application
import com.example.data.local.ArifaDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AiChatRepository
import com.example.data.repository.NetworkMonitor
import com.example.data.repository.SensorManagerHelper

class ArifaApplication : Application() {
    val database by lazy { ArifaDatabase.getDatabase(this) }
    val preferencesManager by lazy { UserPreferencesManager(this) }
    val networkMonitor by lazy { NetworkMonitor(this) }
    val sensorHelper by lazy { SensorManagerHelper(this) }
    val chatRepository by lazy { AiChatRepository(database.chatDao(), preferencesManager) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ArifaApplication
            private set
    }
}
