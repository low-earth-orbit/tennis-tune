package ca.unb.mobiledev.tennis_tune

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import ca.unb.mobiledev.tennis_tune.databinding.AddEditRacquetBinding
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModelFactory

class AddEditRacquetActivity : AppCompatActivity() {
    private lateinit var binding: AddEditRacquetBinding
    private var originalRacquetName: String? = null
    private var originalRacquetHeadSize: String? = null
    private var originalStringMassDensity: String? = null

    private val viewModel: RacquetViewModel by viewModels {
        RacquetViewModelFactory(application)
    }

    private var racquetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddEditRacquetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = getString(R.string.add_racquet)
        binding.editRacquetButtonSave.text = getString(R.string.add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        racquetId = intent.getIntExtra("EXTRA_RACQUET_ID", -1).takeIf { it != -1 }
        racquetId?.let { it ->
            // Fetch and display existing racquet details
            viewModel.getRacquetById(it).observe(this) { racquet ->
                racquet?.let { setupForEdit(it) }
            }
        }

        setupListeners()
        setupInputValidation()
    }

    private fun setupListeners() {
        binding.editRacquetRacquetHeadSize.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.editRacquetStringMassDensity.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.editRacquetRacquetName.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.editRacquetButtonSave.setOnClickListener {
            if (hasUnsavedChanges() && isInputValid()) {
                saveRacquet()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.invalid_inputs), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupInputValidation() {
        val headSizeView = binding.editRacquetRacquetHeadSize
        val stringMassDensityView = binding.editRacquetStringMassDensity
        val racquetNameView = binding.editRacquetRacquetName

        binding.editRacquetRacquetName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val racquetName = s.toString()
                if (racquetName.isEmpty()) {
                    racquetNameView.error = getString(R.string.racquet_name_cannot_be_empty)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        binding.editRacquetRacquetHeadSize.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val headSize = s.toString().toDoubleOrNull()
                if (headSize == null || !isValidHeadSize(headSize)) {
                    headSizeView.error = getString(R.string.supported_head_size)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        binding.editRacquetStringMassDensity.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val density = s.toString().toDoubleOrNull()
                if (density == null || !isValidStringMassDensity(density)) {
                    stringMassDensityView.error =
                        getString(R.string.string_mass_density_accepted_range)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupForEdit(racquet: Racquet) {
        supportActionBar?.title = getString(R.string.edit_racquet)
        binding.editRacquetButtonSave.text = getString(R.string.button_save)
        binding.editRacquetRacquetName.setText(racquet.name)
        binding.editRacquetRacquetHeadSize.setText(racquet.headSize.toString())
        binding.editRacquetStringMassDensity.setText(racquet.stringMassDensity.toString())

        originalRacquetName = racquet.name
        originalRacquetHeadSize = racquet.headSize.toString()
        originalStringMassDensity = racquet.stringMassDensity.toString()
        updateSaveButtonState()
    }

    private fun saveRacquet() {
        val racquetName = binding.editRacquetRacquetName.text.toString()
        val headSize =
            binding.editRacquetRacquetHeadSize.text.toString().toDoubleOrNull()
        val stringMassDensity =
            binding.editRacquetStringMassDensity.text.toString().toDoubleOrNull()

        if (headSize != null && stringMassDensity != null) {
            if (racquetId == null) {
                val newRacquet = Racquet(0, racquetName, headSize, stringMassDensity)
                viewModel.insert(newRacquet)
            } else {
                val racquetToUpdate = Racquet(racquetId!!, racquetName, headSize, stringMassDensity)
                viewModel.update(racquetToUpdate)
            }
            finish() // Close the activity after saving
        } else {
            // Show an error message or handle input validation
        }
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

    private fun isInputValid(): Boolean {
        val racquetName = binding.editRacquetRacquetName.text.toString()
        val headSize = binding.editRacquetRacquetHeadSize.text.toString().toDoubleOrNull()
        val stringDensity = binding.editRacquetStringMassDensity.text.toString().toDoubleOrNull()

        return when {
            racquetName.isEmpty() -> {
                binding.editRacquetRacquetName.error =
                    getString(R.string.racquet_name_cannot_be_empty)
                false
            }

            headSize == null || !isValidHeadSize(headSize) -> {
                binding.editRacquetRacquetHeadSize.error = getString(R.string.supported_head_size)
                false
            }

            stringDensity == null || !isValidStringMassDensity(stringDensity) -> {
                binding.editRacquetStringMassDensity.error =
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

    private fun hasUnsavedChanges(): Boolean {
        val currentRacquetName = binding.editRacquetRacquetName.text.toString()
        val currentRacquetHeadSize = binding.editRacquetRacquetHeadSize.text.toString()
        val currentStringMassDensity = binding.editRacquetStringMassDensity.text.toString()

        return currentRacquetName != originalRacquetName ||
                currentRacquetHeadSize != originalRacquetHeadSize ||
                currentStringMassDensity != originalStringMassDensity
    }

    private fun updateSaveButtonState() {
        binding.editRacquetButtonSave.isEnabled = hasUnsavedChanges() && isInputValid()
    }
}
