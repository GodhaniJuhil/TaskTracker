package com.tasktracker.app.di

import com.tasktracker.app.notification.NotificationHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationHelper(androidContext()) }
}
