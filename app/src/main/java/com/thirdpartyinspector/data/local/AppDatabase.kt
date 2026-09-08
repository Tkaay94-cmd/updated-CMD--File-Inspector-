kotlin
package com.thirdpartyinspector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thirdpartyinspector.data.local.entities.ScanEntity
import com.thirdpartyinspector.data.local.entities.FindingEntity

@Database(
    entities = [ScanEntity::class, FindingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}
