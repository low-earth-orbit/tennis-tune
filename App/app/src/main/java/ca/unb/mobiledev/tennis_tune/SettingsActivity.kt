package ca.unb.mobiledev.tennis_tune

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import ca.unb.mobiledev.tennis_tune.databinding.SettingsPageBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // TODO: Remove Go Back button if not needed as per UI design
        binding.buttonBack.setOnClickListener {
            saveSettings()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            saveSettings()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get the selected unit
        val selectedUnit = if (binding.rbLb.isChecked) "lb" else "kg"

        // Get the racquet head size and string mass density values
        val racquetHeadSize = binding.etRacquetHeadSize.text.toString()
        val stringMassDensity = binding.etStringMassDensity.text.toString()

        // Store the settings in SharedPreferences
        editor.putString("DISPLAY_UNIT", selectedUnit)
        editor.putString("RACQUET_HEAD_SIZE", racquetHeadSize)
        editor.putString("STRING_MASS_DENSITY", stringMassDensity)
        editor.apply()
    }
}
