package com.tasktracker.app

import android.app.Application
import com.tasktracker.app.di.initKoin
import com.tasktracker.app.di.notificationModule
import com.tasktracker.app.notification.NotificationHelper
import com.tasktracker.app.notification.TaskChangeNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class TaskTrackerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TaskTrackerApplication)
            modules(notificationModule)
        }
        get<NotificationHelper>().createChannel()
        get<TaskChangeNotifier>().start(appScope)
    }
}
