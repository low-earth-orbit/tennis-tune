package ca.unb.mobiledev.tennis_tune.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RacquetViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RacquetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RacquetViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}