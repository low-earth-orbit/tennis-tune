package ca.unb.mobiledev.tennis_tune

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ca.unb.mobiledev.tennis_tune.databinding.SettingsPageBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsPageBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var originalDisplayUnit: String? = null

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
    }

    private fun setupListeners() {
        binding.rgDisplayUnit.setOnCheckedChangeListener { _, _ ->
            updateSaveButtonState()
        }

        binding.settingsButtonSave.setOnClickListener {
            if (hasUnsavedChanges()) {
                saveSettings()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.invalid_inputs), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSettings() {
        originalDisplayUnit = sharedPreferences.getString("DISPLAY_UNIT", "lb")

        binding.rbLb.isChecked = (originalDisplayUnit == "lb")
        binding.rbKg.isChecked = (originalDisplayUnit == "kg")

        updateSaveButtonState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            if (hasUnsavedChanges()) {
                AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
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

        // Store the settings in SharedPreferences
        editor.putString("DISPLAY_UNIT", selectedUnit)
        editor.apply()
    }

    private fun hasUnsavedChanges(): Boolean {
        val currentDisplayUnit = if (binding.rbLb.isChecked) "lb" else "kg"
        return currentDisplayUnit != originalDisplayUnit
    }

    private fun updateSaveButtonState() {
        binding.settingsButtonSave.isEnabled = hasUnsavedChanges()
    }
}
