package ca.unb.mobiledev.tennis_tune.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.repository.RacquetRepository
import kotlinx.coroutines.launch

class RacquetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RacquetRepository = RacquetRepository(application)

    val allRacquets: LiveData<List<Racquet>> = repository.getAllRacquets()

    init {
        checkAndInsertDefaultRacquet()
    }

    private fun checkAndInsertDefaultRacquet() {
        viewModelScope.launch {
            val racquets = repository.getAllRacquetsSynchronously()
            if (racquets.isEmpty()) {
                val defaultRacquet = Racquet.defaultRacquet()
                val insertedId = repository.insert(defaultRacquet)
                saveSelectedRacquetId(getApplication<Application>().applicationContext, insertedId)
            }
        }
    }

    fun insert(racquet: Racquet) = viewModelScope.launch {
        repository.insert(racquet)
    }

    fun deleteRacquet(racquet: Racquet) = viewModelScope.launch {
        if (allRacquets.value.orEmpty().size > 1) {
            repository.delete(racquet)
        }
    }

    companion object {
        fun saveSelectedRacquetId(context: Context, selectedRacquetId: Int) {
            val sharedPreferences =
                context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt("SELECTED_RACQUET_ID", selectedRacquetId)
                apply()
            }
        }
    }
}