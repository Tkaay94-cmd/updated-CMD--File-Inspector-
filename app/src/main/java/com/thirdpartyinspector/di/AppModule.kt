package com.thirdpartyinspector.di

import android.content.Context
import androidx.room.Room
import com.thirdpartyinspector.data.local.AppDatabase
import com.thirdpartyinspector.data.local.ScanDao
import com.thirdpartyinspector.scanner.*
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

    @Provides
    @Singleton
    fun providePackageInspector(@ApplicationContext context: Context): PackageInspector = PackageInspector(context)

    @Provides
    @Singleton
    fun providePermissionInspector(): PermissionInspector = PermissionInspector()

    @Provides
    @Singleton
    fun provideAccessibilityInspector(@ApplicationContext context: Context): AccessibilityInspector = AccessibilityInspector(context)

    @Provides
    @Singleton
    fun provideDeviceAdminInspector(@ApplicationContext context: Context): DeviceAdminInspector = DeviceAdminInspector(context)

    @Provides
    @Singleton
    fun provideRootEnvironmentInspector(@ApplicationContext context: Context): RootEnvironmentInspector = RootEnvironmentInspector(context)

    @Provides
    @Singleton
    fun provideFileSystemInspector(@ApplicationContext context: Context): FileSystemInspector = FileSystemInspector(context)

    @Provides
    @Singleton
    fun provideRiskEngine(permissionInspector: PermissionInspector): RiskEngine = RiskEngine(permissionInspector)
}
