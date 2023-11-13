package ca.unb.mobiledev.tennis_tune

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import ca.unb.mobiledev.tennis_tune.databinding.SettingsPageBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsPageBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var originalDisplayUnit: String? = null
    private var originalRacquetHeadSize: String? = null
    private var originalStringMassDensity: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = getString(R.string.title_settings_page)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

        setupListeners()
        loadSettings()
        setupInputValidation()
    }

    private fun setupListeners() {
        binding.etRacquetHeadSize.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.etStringMassDensity.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.rgDisplayUnit.setOnCheckedChangeListener { _, _ ->
            updateSaveButtonState()
        }

        binding.buttonSave.setOnClickListener {
            if (validateInputs()) {
                if (hasUnsavedChanges()) {
                    saveSettings()
                    Toast.makeText(this, getString(R.string.settings_changed), Toast.LENGTH_SHORT)
                        .show()
                }
                finish()
            } else {
                Toast.makeText(this, getString(R.string.invalid_inputs), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSettings() {
        originalDisplayUnit = sharedPreferences.getString("DISPLAY_UNIT", "lb")
        originalRacquetHeadSize = sharedPreferences.getString("RACQUET_HEAD_SIZE", "100")
        originalStringMassDensity = sharedPreferences.getString("STRING_MASS_DENSITY", "1.53")

        binding.rbLb.isChecked = (originalDisplayUnit == "lb")
        binding.rbKg.isChecked = (originalDisplayUnit == "kg")
        binding.etRacquetHeadSize.setText(originalRacquetHeadSize)
        binding.etStringMassDensity.setText(originalStringMassDensity)

        updateSaveButtonState()
    }

    private fun setupInputValidation() {
        val etHeadSize = findViewById<EditText>(R.id.et_racquet_head_size)
        val etStringMassDensity = findViewById<EditText>(R.id.et_string_mass_density)

        binding.etRacquetHeadSize.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val headSize = s.toString().toDoubleOrNull()
                if (headSize == null || !isValidHeadSize(headSize)) {
                    etHeadSize.error = getString(R.string.supported_head_size)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        binding.etStringMassDensity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val density = s.toString().toDoubleOrNull()
                if (density == null || !isValidStringMassDensity(density)) {
                    etStringMassDensity.error =
                        getString(R.string.string_mass_density_accepted_range)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateInputs(): Boolean {
        val headSize = binding.etRacquetHeadSize.text.toString().toDoubleOrNull()
        val stringDensity = binding.etStringMassDensity.text.toString().toDoubleOrNull()

        return when {
            headSize == null || !isValidHeadSize(headSize) -> {
                binding.etRacquetHeadSize.error = getString(R.string.supported_head_size)
                false
            }

            stringDensity == null || !isValidStringMassDensity(stringDensity) -> {
                binding.etStringMassDensity.error =
                    getString(R.string.string_mass_density_accepted_range)
                false
            }

            else -> true
        }
    }

    private fun isValidHeadSize(size: Double): Boolean {
        return size in 85.0..130.0
    }

    private fun isValidStringMassDensity(density: Double): Boolean {
        return density in 0.5..3.0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            if (hasUnsavedChanges()) {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.unsaved_changes_message))
                    .setPositiveButton(getString(R.string.discard)) { _, _ -> onBackPressedDispatcher.onBackPressed() }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            } else {
                finish()
            }
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

    private fun hasUnsavedChanges(): Boolean {
        val currentDisplayUnit = if (binding.rbLb.isChecked) "lb" else "kg"
        val currentRacquetHeadSize = binding.etRacquetHeadSize.text.toString()
        val currentStringMassDensity = binding.etStringMassDensity.text.toString()

        return currentDisplayUnit != originalDisplayUnit ||
                currentRacquetHeadSize != originalRacquetHeadSize ||
                currentStringMassDensity != originalStringMassDensity
    }

    private fun updateSaveButtonState() {
        binding.buttonSave.isEnabled = hasUnsavedChanges()
    }
}
