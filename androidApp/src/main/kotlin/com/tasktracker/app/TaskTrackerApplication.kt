package com.tasktracker.app

import android.app.Application
import com.tasktracker.app.di.initKoin
import com.tasktracker.app.di.notificationModule
import com.tasktracker.app.notification.NotificationHelper
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class TaskTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TaskTrackerApplication)
            modules(notificationModule)
        }
        get<NotificationHelper>().createChannel()
    }
}
