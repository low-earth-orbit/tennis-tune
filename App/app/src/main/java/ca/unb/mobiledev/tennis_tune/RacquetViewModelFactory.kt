import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ca.unb.mobiledev.tennis_tune.repository.RacquetRepository
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel

class RacquetViewModelFactory(private val repository: RacquetRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RacquetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RacquetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}