package com.tasktracker.app.di

import com.tasktracker.app.data.repository.AuthRepositoryImpl
import com.tasktracker.app.data.repository.TaskRepositoryImpl
import com.tasktracker.app.data.repository.TeamRepositoryImpl
import com.tasktracker.app.domain.repository.AuthRepository
import com.tasktracker.app.domain.repository.TaskRepository
import com.tasktracker.app.domain.repository.TeamRepository
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import com.tasktracker.app.presentation.viewmodel.TaskListViewModel
import com.tasktracker.app.presentation.viewmodel.TeamViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.messaging.messaging
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val firebaseModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { Firebase.messaging }
}

val dataModule = module {
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::TeamRepositoryImpl) bind TeamRepository::class
    singleOf(::TaskRepositoryImpl) bind TaskRepository::class
}

val presentationModule = module {
    viewModel { AuthViewModel(get(), getOrNull()) }
    viewModelOf(::TaskListViewModel)
    viewModelOf(::TeamViewModel)
}

val appModules = listOf(firebaseModule, dataModule, presentationModule)
