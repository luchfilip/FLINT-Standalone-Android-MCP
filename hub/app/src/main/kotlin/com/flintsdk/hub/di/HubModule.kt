package com.flintsdk.hub.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for FLINT Hub dependencies.
 *
 * ToolRegistry, HubSettings, HubLogger, McpProtocol, and McpServer
 * are all provided as singletons via their @Singleton @Inject constructor
 * annotations, so they are automatically available in the Hilt graph.
 *
 * This module exists for any future @Provides bindings that cannot
 * use constructor injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object HubModule
