package com.banana.toolbox.di

import com.banana.toolbox.data.repository.AppRepositoryImpl
import com.banana.toolbox.data.repository.FileRepositoryImpl
import com.banana.toolbox.domain.repository.AppRepository
import com.banana.toolbox.domain.repository.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 依赖注入模块 - 仓库绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepositoryImpl: FileRepositoryImpl
    ): FileRepository
    
    @Binds
    @Singleton
    abstract fun bindAppRepository(
        appRepositoryImpl: AppRepositoryImpl
    ): AppRepository
}
