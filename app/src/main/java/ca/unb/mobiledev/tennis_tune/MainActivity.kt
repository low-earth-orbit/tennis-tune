package ca.unb.mobiledev.tennis_tune

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import ca.unb.mobiledev.tennis_tune.databinding.HomePageBinding
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

class MainActivity : AppCompatActivity(), VisualizerView.OnDisplayFrequencyChangeListener {

    private lateinit var binding: HomePageBinding
    private var mVisualizerView: VisualizerView? = null
    private var tensionTextView: TextView? = null
    private var unitTextView: TextView? = null

    // Parameters for tension calculation
    private var displayUnit: String = "lb"
    private var racquetHeadSize: Double = 100.0
    private var stringMassDensity: Double = 1.53

    // Audio processing
    private lateinit var audioRecord: AudioRecord
    private var bufferSizeInBytes: Int = 0
    private val recordAudioPermission = 1
    private val visualizerHeightDip = 50f
    private var job = Job()

    // Database connectivity
    private lateinit var viewModel: RacquetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarHome)
        supportActionBar?.title = getString(R.string.title_home_page)

        viewModel = ViewModelProvider(
            this,
            RacquetViewModelFactory(application)
        )[RacquetViewModel::class.java]

        initUI()
        initAudioRecord()
        setupVisualizer()
    }

    override fun onStart() {
        super.onStart()
        // Initialize settings from shared preferences
        loadSettings()
        // Load racquet specs
        loadSelectedRacquetDetails()
    }

    override fun onResume() {
        // onResume() is called when the activity comes into the foreground
        super.onResume()
        // Check audio permission & start audio recording
        checkRecordAudioPermission()
    }

    override fun onPause() {
        super.onPause()

        // Reset frequency text
        resetFrequencyText()

        // Stop the recording and release resources
        stopAudioCapture()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord.release()
    }

    private fun initUI() {
        tensionTextView = binding.tensionTextView
        unitTextView = binding.unitTextView

        binding.resetButton.setOnClickListener {
            resetFrequencyText()
        }

        binding.racquetsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, RacquetListActivity::class.java)
            startActivity(intent)
        }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initAudioRecord() {
        val audioSource = MediaRecorder.AudioSource.MIC
        val sampleRateInHz = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        bufferSizeInBytes =
            AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

        audioRecord =
            AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
    }

    private fun stopAudioCapture() {
        // Cancel the coroutine job
        job.cancel()

        if (::audioRecord.isInitialized && audioRecord.state == AudioRecord.STATE_INITIALIZED && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop()
        }
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        displayUnit = sharedPreferences.getString("DISPLAY_UNIT", "lb")!!
    }

    private fun loadSelectedRacquetDetails() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedRacquetId = sharedPreferences.getInt("SELECTED_RACQUET_ID", -1)

        if (selectedRacquetId != -1) {
            viewModel.getRacquetById(selectedRacquetId).observe(this) { racquet ->
                if (racquet != null) {
                    racquetHeadSize = racquet.headSize
                    stringMassDensity = racquet.stringMassDensity
                } else {
                    Log.i("MainActivity", "Racquet not found for ID: $selectedRacquetId")
                }
            }
        }
    }

    private fun resetFrequencyText() {
        mVisualizerView?.resetFrequencies()
        tensionTextView?.text = this.getString(R.string.text_detecting)
        unitTextView?.text = ""
    }

    private fun setupVisualizer() {
        mVisualizerView = VisualizerView(this)
        mVisualizerView?.displayFrequencyListener = this
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (this.visualizerHeightDip * resources.displayMetrics.density).toInt()
        )
        val visualizerContainer = findViewById<LinearLayout>(R.id.visualizer_container)
        visualizerContainer?.addView(mVisualizerView)
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        if (::audioRecord.isInitialized && audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            initAudioRecord() // Re-initialize if necessary
        }

        val buffer = ByteArray(bufferSizeInBytes)
        job = Job() // Recreate the job object
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            audioRecord.startRecording()
            mVisualizerView?.setAudioInputAvailable(true)

            while (isActive) {
                val readSize = audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                mVisualizerView?.updateVisualizer(buffer.sliceArray(0 until readSize))
            }
            audioRecord.stop()
            mVisualizerView?.setAudioInputAvailable(false)
        }
    }

    private fun checkRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission was denied previously and the user checked "Don't ask again"
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                // Show an explanation to the user
                Toast.makeText(
                    this,
                    "We need audio permission for string tension measurement",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioPermission
            )
        } else {
            // Permission has already been granted, proceed with audio capture
            startAudioCapture()
        }
    }

    override fun onDisplayFrequencyChange(frequency: Float) {
        Log.d("MainActivity", "Display Frequency: $frequency")
        Log.d("MainActivity", "racquetHeadSize: $racquetHeadSize")
        Log.d("MainActivity", "stringMassDensity: $stringMassDensity")

        val tensionLb =
            frequencyToTension(frequency, racquetHeadSize, stringMassDensity)
        val tensionDisplay = if (displayUnit == "kg") {
            val tensionKg = tensionLb * 0.45359237
            "%.1f".format(tensionKg)
        } else {
            "%.1f".format(tensionLb)
        }

        val unitDisplay = if (displayUnit == "kg") {
            "kg"
        } else {
            "lb"
        }

        runOnUiThread {
            tensionTextView?.text = buildString { append(tensionDisplay) }
            unitTextView?.text = buildString { append(unitDisplay) }
        }
    }

    private fun frequencyToTension(
        frequency: Float,
        racquetHeadSize: Double,
        stringMassDensity: Double
    ): Double {
        val headSizeSqM = racquetHeadSize * 0.00064516
        val densityKgM = stringMassDensity / 1000.0
        val toSingleStringFreqFactor = 1.0121457 // Conversion factor from elliptical membrane
        // to square area tension. The conversion logic is based on square area assumption
        val toMachineTensionFactor = 1.470588235 // Machine
        // tension is the pull tension. Actual tension is about 32% lower than machine tension,
        // immediately after
        // strung. Despite this fact, to avoid confusion, display tension in the app is to
        // approximate the machine tension, thus requiring this conversion factor.
        val newtonToLbFactor = 0.2248089431
        val tensionNewton = 4 * headSizeSqM * densityKgM * (frequency *
                toSingleStringFreqFactor).pow(2)
        return tensionNewton * toMachineTensionFactor * newtonToLbFactor
    }
}
