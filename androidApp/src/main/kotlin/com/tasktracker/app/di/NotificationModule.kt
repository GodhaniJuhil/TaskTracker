package com.tasktracker.app.di

import com.tasktracker.app.notification.NotificationHelper
import com.tasktracker.app.notification.TaskChangeNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationHelper(androidContext()) }
    singleOf(::TaskChangeNotifier)
}
