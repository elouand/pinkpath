package com.traveling.di

import com.traveling.data.repository.ItineraryRepositoryImpl
import com.traveling.data.repository.PlaceRepositoryImpl
import com.traveling.data.repository.PostRepositoryImpl
import com.traveling.domain.repository.ItineraryRepository
import com.traveling.domain.repository.PlaceRepository
import com.traveling.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(
        placeRepositoryImpl: PlaceRepositoryImpl
    ): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindItineraryRepository(
        itineraryRepositoryImpl: ItineraryRepositoryImpl
    ): ItineraryRepository
}
