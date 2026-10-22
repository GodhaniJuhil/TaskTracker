package com.tasktracker.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Starts Koin once per process. Android passes an extra declaration (e.g. `androidContext`);
 * iOS calls it without arguments from Swift as `KoinInitKt.initKoin()`.
 */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        config?.invoke(this)
        modules(appModules)
    }
