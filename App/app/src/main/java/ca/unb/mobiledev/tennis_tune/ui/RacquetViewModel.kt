package ca.unb.mobiledev.tennis_tune.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.repository.RacquetRepository
import kotlinx.coroutines.launch

class RacquetViewModel(application: Application) : AndroidViewModel(application) {
    private val racquetRepository: RacquetRepository = RacquetRepository(application)

    fun insert(racquet: Racquet) = viewModelScope.launch {
        racquetRepository.insert(racquet)
    }
}
