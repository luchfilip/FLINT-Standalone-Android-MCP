package com.flintsdk.hub.di

import com.flintsdk.hub.logging.HubLogger
import com.flintsdk.hub.logging.Logger
import com.flintsdk.hub.service.HubServiceController
import com.flintsdk.hub.service.HubServiceControllerImpl
import com.flintsdk.hub.ui.setup.PermissionChecker
import com.flintsdk.hub.ui.setup.PermissionCheckerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HubModule {

    @Binds
    abstract fun bindLogger(impl: HubLogger): Logger

    @Binds
    abstract fun bindHubServiceController(impl: HubServiceControllerImpl): HubServiceController

    @Binds
    abstract fun bindPermissionChecker(impl: PermissionCheckerImpl): PermissionChecker
}
