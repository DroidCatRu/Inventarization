package ru.droidcat.inventarization.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun itemDao(): ItemDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "inventarization-db"
                    ).build()

                    INSTANCE = instance
                    instance
                }
        }
    }
}