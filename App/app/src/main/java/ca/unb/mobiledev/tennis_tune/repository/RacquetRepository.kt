package ca.unb.mobiledev.tennis_tune.repository

import android.app.Application
import androidx.lifecycle.LiveData
import ca.unb.mobiledev.tennis_tune.dao.RacquetDao
import ca.unb.mobiledev.tennis_tune.db.AppDatabase.Companion.getDatabase
import ca.unb.mobiledev.tennis_tune.entity.Racquet

class RacquetRepository(application: Application) {
    private val racquetDao: RacquetDao = getDatabase(application).racquetDao()

    suspend fun insert(racquet: Racquet) {
        return racquetDao.insert(racquet)
    }

    suspend fun getAllRacquetsSynchronously(): List<Racquet> {
        return racquetDao.getAllRacquetsSynchronously()
    }

    fun getAllRacquets(): LiveData<List<Racquet>> {
        return racquetDao.getAllRacquets()
    }

    suspend fun delete(racquet: Racquet) {
        racquetDao.delete(racquet)
    }

    suspend fun checkAndInsertDefaultRacquet() {
        val racquets = getAllRacquetsSynchronously()
        if (racquets.isEmpty()) {
            val defaultRacquet = Racquet.defaultRacquet()
            insert(defaultRacquet)
        }
    }
}
