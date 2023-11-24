package ca.unb.mobiledev.tennis_tune

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ca.unb.mobiledev.tennis_tune.databinding.AddEditRacquetBinding
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModelFactory

class AddEditRacquetActivity : AppCompatActivity() {
    private lateinit var binding: AddEditRacquetBinding

    private val viewModel: RacquetViewModel by viewModels {
        RacquetViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddEditRacquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = getString(R.string.add_edit_racquet)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.editRacquetButtonSave.setOnClickListener {
            saveRacquet()
        }
    }

    private fun saveRacquet() {
        val racquetName = binding.editRacquetRacquetName.text.toString()
        val headSize =
            binding.editRacquetRacquetHeadSize.text.toString().toDoubleOrNull()
        val stringMassDensity =
            binding.editRacquetStringMassDensity.text.toString().toDoubleOrNull()

        if (headSize != null && stringMassDensity != null) {
            val racquet = Racquet(0, racquetName, headSize, stringMassDensity)
            viewModel.insert(racquet)
            finish() // Close the activity after saving
        } else {
            // Show an error message or handle input validation
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
