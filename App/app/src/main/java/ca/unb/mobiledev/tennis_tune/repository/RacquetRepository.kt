package ca.unb.mobiledev.tennis_tune.repository

import android.app.Application
import ca.unb.mobiledev.tennis_tune.dao.RacquetDao
import ca.unb.mobiledev.tennis_tune.db.AppDatabase
import ca.unb.mobiledev.tennis_tune.db.AppDatabase.Companion.getDatabase
import ca.unb.mobiledev.tennis_tune.entity.Racquet

class RacquetRepository(application: Application) {
    private val racquetDao: RacquetDao? = getDatabase(application).racquetDao()

    fun insert(racquet: Racquet) {
        // Using a Runnable thread object as there are no return values
        AppDatabase.databaseWriterExecutor.execute { racquetDao!!.insert(racquet) }
    }
}
