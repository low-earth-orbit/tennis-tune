package ca.unb.mobiledev.tennis_tune.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ca.unb.mobiledev.tennis_tune.entity.Racquet

@Dao
interface RacquetDao {
    @Query("SELECT * FROM racquets ORDER BY `order`")
    suspend fun getAllRacquetsSynchronously(): List<Racquet>

    @Query("SELECT * FROM racquets ORDER BY `order`")
    fun getAllRacquets(): LiveData<List<Racquet>>

    @Query("SELECT * FROM racquets WHERE id = :id")
    fun getById(id: Int): LiveData<Racquet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(racquet: Racquet)

    @Update
    suspend fun update(racquet: Racquet)

    @Delete
    suspend fun delete(racquet: Racquet)

    @Query("DELETE FROM racquets WHERE id = :id")
    fun deleteById(id: Int)
}