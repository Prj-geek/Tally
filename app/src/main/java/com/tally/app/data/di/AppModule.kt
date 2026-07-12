package com.tally.app.data.di

import android.content.Context
import com.tally.app.BuildConfig
import com.tally.app.data.local.TallyDatabase
import com.tally.app.data.local.dao.WatchHistoryDao
import com.tally.app.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TallyDatabase =
        TallyDatabase.create(context)

    @Provides
    fun provideWatchlistDao(db: TallyDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideWatchHistoryDao(db: TallyDatabase): WatchHistoryDao = db.watchHistoryDao()
}
