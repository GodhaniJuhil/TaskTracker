package com.tasktracker.app.di

import com.tasktracker.app.data.repository.AuthRepositoryImpl
import com.tasktracker.app.data.repository.InvitationRepositoryImpl
import com.tasktracker.app.data.repository.NotificationRepositoryImpl
import com.tasktracker.app.data.repository.TaskRepositoryImpl
import com.tasktracker.app.data.repository.WorkspaceRepositoryImpl
import com.tasktracker.app.domain.repository.AuthRepository
import com.tasktracker.app.domain.repository.InvitationRepository
import com.tasktracker.app.domain.repository.NotificationRepository
import com.tasktracker.app.domain.repository.TaskRepository
import com.tasktracker.app.domain.repository.WorkspaceRepository
import com.tasktracker.app.presentation.viewmodel.AuthViewModel
import com.tasktracker.app.presentation.viewmodel.InvitesViewModel
import com.tasktracker.app.presentation.viewmodel.NotificationsViewModel
import com.tasktracker.app.presentation.viewmodel.TaskListViewModel
import com.tasktracker.app.presentation.viewmodel.WorkspaceViewModel
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
    singleOf(::WorkspaceRepositoryImpl) bind WorkspaceRepository::class
    singleOf(::TaskRepositoryImpl) bind TaskRepository::class
    singleOf(::InvitationRepositoryImpl) bind InvitationRepository::class
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
}

val presentationModule = module {
    viewModel { AuthViewModel(get(), getOrNull()) }
    viewModelOf(::TaskListViewModel)
    viewModelOf(::WorkspaceViewModel)
    viewModelOf(::InvitesViewModel)
    viewModelOf(::NotificationsViewModel)
}

val appModules = listOf(firebaseModule, dataModule, presentationModule)
