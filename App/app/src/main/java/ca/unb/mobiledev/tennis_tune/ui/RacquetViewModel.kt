package ca.unb.mobiledev.tennis_tune.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.repository.RacquetRepository
import kotlinx.coroutines.launch

class RacquetViewModel(application: Application) : AndroidViewModel(application) {
    private val racquetRepository: RacquetRepository = RacquetRepository(application)

    // LiveData holding the list of racquets
    val allRacquets: LiveData<List<Racquet>> = racquetRepository.getAllRacquets()

    fun insert(racquet: Racquet) = viewModelScope.launch {
        racquetRepository.insert(racquet)
    }

    fun delete(racquet: Racquet) = viewModelScope.launch {
        racquetRepository.delete(racquet)
    }
}