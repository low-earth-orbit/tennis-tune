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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ca.unb.mobiledev.tennis_tune.databinding.HomePageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

class MainActivity : AppCompatActivity(), VisualizerView.OnDisplayFrequencyChangeListener {

    private lateinit var binding: HomePageBinding
    private val recordAudioPermission = 1
    private val visualizerHeightDip = 50f
    private var mVisualizerView: VisualizerView? = null
    private var frequencyTextView: TextView? = null
    private var job = Job()

    // Settings variables declaration
    private var displayUnit: String? = null
    private var racquetHeadSize: Double? = null
    private var stringMassDensity: Double? = null

    private lateinit var audioRecord: AudioRecord
    private var bufferSizeInBytes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarHome)
        supportActionBar?.title = getString(R.string.title_home_page)

        initUI()
        initAudioRecord()
        setupVisualizer()
    }

    override fun onStart() {
        super.onStart()
        // Initialize settings from shared preferences
        loadSettings()
    }

    override fun onResume() {
        // onResume() is called when the activity comes into the foreground
        super.onResume()
        // Check audio permission & start audio recording
        checkRecordAudioPermission()
    }

    override fun onPause() {
        super.onPause()
        // Stop the recording and release resources
        stopAudioCapture()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord.release()
    }

    private fun initUI() {
        frequencyTextView = binding.frequencyTextView

        binding.resetButton.setOnClickListener {
            resetFrequencyText()
        }

        val racquetsButton = Button(this).apply {
            text = getString(R.string.racquets)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val intent = Intent(this@MainActivity, RacquetListActivity::class.java)
                startActivity(intent)
            }
        }
        binding.bottomMenu.addView(racquetsButton)

        val settingsButton = Button(this).apply {
            text = getString(R.string.button_settings)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        binding.bottomMenu.addView(settingsButton)
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
        // Previous settings values before loading new ones
        val prevDisplayUnit = displayUnit
        val prevRacquetHeadSize = racquetHeadSize
        val prevStringMassDensity = stringMassDensity

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        displayUnit = sharedPreferences.getString("DISPLAY_UNIT", "lb")
        racquetHeadSize = sharedPreferences.getString("RACQUET_HEAD_SIZE", "100")?.toDouble()
        stringMassDensity = sharedPreferences.getString("STRING_MASS_DENSITY", "1.50")?.toDouble()

        // Check if any setting value has changed
        if (prevDisplayUnit != displayUnit || prevRacquetHeadSize != racquetHeadSize || prevStringMassDensity != stringMassDensity) {
            // Reset the text as the settings have changed
            resetFrequencyText()
        }
    }

    private fun resetFrequencyText() {
        mVisualizerView?.resetFrequencies()
        frequencyTextView?.text = this.getString(R.string.text_detecting)
    }

    private fun setupVisualizer() {
        mVisualizerView = VisualizerView(this)
        mVisualizerView?.displayFrequencyListener = this
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (this.visualizerHeightDip * resources.displayMetrics.density).toInt()
        )
        val visualizerContainer = findViewById<LinearLayout>(R.id.my_visualizer_container)
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

        if (racquetHeadSize != null && stringMassDensity != null) {
            val tensionLb = frequencyToTension(frequency, racquetHeadSize!!, stringMassDensity!!)
            val tensionDisplay = if (displayUnit == "kg") {
                val tensionKg = tensionLb * 0.45359237
                "%.1f kg".format(tensionKg)
            } else {
                "%.1f lb".format(tensionLb)
            }

            runOnUiThread {
                frequencyTextView?.text = buildString {
                    append("Frequency: ${"%.0f".format(frequency)} Hz\nTension: $tensionDisplay")
                }
            }
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
