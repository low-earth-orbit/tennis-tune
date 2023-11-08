package ca.unb.mobiledev.tennis_tune

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private var mVisualizer: Visualizer? = null
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var frequencyTextView: TextView? = null
    private val job = Job()
    private lateinit var resetButton: Button

    // Settings variables declaration
    private var displayUnit: String? = null
    private var racquetHeadSize: Double? = null
    private var stringMassDensity: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize settings from shared preferences
        loadSettings()

        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarHome)
        supportActionBar?.title = "Home"

        frequencyTextView = findViewById(R.id.frequencyTextView)
        setupVisualizerFxAndUI()
        mVisualizerView?.displayFrequencyListener = this

        checkRecordAudioPermission()

        resetButton = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            resetVisualizer()
        }

        val setUpButton = Button(this).apply {
            text = "Set Up"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        findViewById<LinearLayout>(R.id.bottomMenu).addView(setUpButton)
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    override fun onStart() {
        super.onStart()
        loadSettings()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
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
            // Reset the visualizer and frequency text as the settings have changed
            resetVisualizer()
        }
    }

    private fun resetVisualizer() {
        mVisualizerView?.resetFrequencies()
        frequencyTextView?.text = "Detecting..."
    }

    private fun setupVisualizerFxAndUI() {
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (this.visualizerHeightDip * resources.displayMetrics.density).toInt()
        )
        mLinearLayout = findViewById(R.id.my_visualizer_container)
        mLinearLayout?.addView(mVisualizerView)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            mVisualizer?.release()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            recordAudioPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, start audio capture
                    startAudioCapture()
                } else {
                    // Permission denied, disable the functionality that depends on this permission.
                    mVisualizerView?.setAudioInputAvailable(false)
                    Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun startAudioCapture() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            val audioSource = MediaRecorder.AudioSource.MIC
            val sampleRateInHz = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_8BIT
            val bufferSizeInBytes =
                AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

            val audioRecord =
                AudioRecord(
                    audioSource,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes
                )

            val buffer = ByteArray(bufferSizeInBytes)
            val scope = CoroutineScope(Dispatchers.IO + job)

            scope.launch {
                audioRecord.startRecording()
                mVisualizerView?.setAudioInputAvailable(true)

                while (isActive) {
                    val readSize =
                        audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    mVisualizerView?.updateVisualizer(buffer.sliceArray(0 until readSize))
                }
                audioRecord.stop()
                mVisualizerView?.setAudioInputAvailable(false)
            }
        } else {
            // Handle the case where permission is not granted
            mVisualizerView?.setAudioInputAvailable(false)
            Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed for string tension measurement")
                    .setPositiveButton("Ok") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            recordAudioPermission
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create().show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    recordAudioPermission
                )
            }
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
