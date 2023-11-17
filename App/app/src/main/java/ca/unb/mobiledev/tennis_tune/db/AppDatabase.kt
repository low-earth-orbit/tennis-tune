package ca.unb.mobiledev.tennis_tune.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ca.unb.mobiledev.tennis_tune.dao.RacquetDao
import ca.unb.mobiledev.tennis_tune.entity.Racquet

@Database(entities = [Racquet::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun racquetDao(): RacquetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "racquet_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
