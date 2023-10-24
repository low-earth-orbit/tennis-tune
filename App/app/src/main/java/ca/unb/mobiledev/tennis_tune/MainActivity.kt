//Source for visualizer and audio recording code: https://github.com/romerojhh/androidAudioVisualizer
package ca.unb.mobiledev.tennis_tune

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.unb.mobiledev.tennis_tune.databinding.ActivityMainBinding
import ca.unb.mobiledev.tennis_tune.theme.ApiexplorationTheme
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val MY_PERMISSIONS_RECORD_AUDIO = 1
    private val VISUALIZER_HEIGHT_DIP = 50f
    private var mVisualizer: Visualizer? = null
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var mStatusTextView: TextView? = null
    private val job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupVisualizerFxAndUI()
        checkRecordAudioPermission()

        startAudioCapture()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupVisualizerFxAndUI() {
        // Create a ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.VisualizerView
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            (this.VISUALIZER_HEIGHT_DIP * resources.displayMetrics.density).toInt()
        )
        // Get the LinearLayout container and add the ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.ca.unb.mobiledev.tennis_tune.VisualizerView to it
        mLinearLayout = findViewById(R.id.my_visualizer_container)
        mLinearLayout?.addView(mVisualizerView)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            mVisualizer?.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val audioSource = MediaRecorder.AudioSource.MIC
        val sampleRateInHz = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        val bufferSizeInBytes =
            AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        val audioRecord =
            AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
        val buffer = ByteArray(bufferSizeInBytes)
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
            // Permission has not been granted yet, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_RECORD_AUDIO
            )
        } else {
            // Permission has already been granted, proceed with audio capture
            startAudioCapture()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted, proceed with audio capture
                startAudioCapture()
            } else {
                // Permission has been denied, show a message to the user or handle the error
                Toast.makeText(this, "Audio capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ApiexplorationTheme {
        Greeting("Android")
    }
}