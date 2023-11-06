package ca.unb.mobiledev.tennis_tune

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import ca.unb.mobiledev.tennis_tune.databinding.SettingsPageBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsPageBinding

    private lateinit var sharedPreferences: SharedPreferences
    private var mVisualizerView: VisualizerView? = null
    private var frequencyTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        binding.rgDisplayUnit.setOnCheckedChangeListener { _, checkedId ->
            sharedPreferences.edit()
                .putString("DISPLAY_UNIT", if (checkedId == R.id.rb_lb) "lb" else "kg").apply()
        }

        binding.etRacquetHeadSize.addTextChangedListener {
            sharedPreferences.edit().putString("RACQUET_HEAD_SIZE", it.toString()).apply()
        }

        binding.etStringMassDensity.addTextChangedListener {
            sharedPreferences.edit().putString("STRING_MASS_DENSITY", it.toString()).apply()
        }

        // TODO: Remove Go Back button if not needed as per UI design
        binding.buttonBack.setOnClickListener {
            onSettingsChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            onSettingsChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onSettingsChanged() {
        saveSettings()
        mVisualizerView?.resetFrequencies()
        frequencyTextView?.text = "Detecting..."
        finish()
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

    private fun loadSettings() {
        when (sharedPreferences.getString("DISPLAY_UNIT", "lb")) {
            "lb" -> binding.rbLb.isChecked = true
            "kg" -> binding.rbKg.isChecked = true
        }

        binding.etRacquetHeadSize.setText(sharedPreferences.getString("RACQUET_HEAD_SIZE", "100"))
        binding.etStringMassDensity.setText(
            sharedPreferences.getString(
                "STRING_MASS_DENSITY",
                "1.50"
            )
        )
    }
}
