package ca.unb.mobiledev.tennis_tune.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.repository.RacquetRepository
import kotlinx.coroutines.launch

class RacquetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RacquetRepository = RacquetRepository(application)

    // LiveData holding the list of racquets
    val allRacquets: LiveData<List<Racquet>> = repository.getAllRacquets()

    fun insert(racquet: Racquet) = viewModelScope.launch {
        repository.insert(racquet)
    }

    fun deleteRacquet(racquet: Racquet) = viewModelScope.launch {
        if (allRacquets.value.orEmpty().size > 1) {
            repository.delete(racquet)
            // Additional logic to select the next racquet
        } else {
            // Handle the case where there's only one racquet
        }
    }
}