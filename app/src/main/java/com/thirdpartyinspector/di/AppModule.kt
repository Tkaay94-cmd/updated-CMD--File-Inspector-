kotlin
package com.thirdpartyinspector.di

import android.content.Context
import androidx.room.Room
import com.thirdpartyinspector.data.local.AppDatabase
import com.thirdpartyinspector.data.local.ScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "third_party_inspector_db"
        ).build()
    }

    @Provides
    fun provideScanDao(db: AppDatabase): ScanDao = db.scanDao()
}
