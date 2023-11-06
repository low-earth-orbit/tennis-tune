package ca.unb.mobiledev.tennis_tune

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ca.unb.mobiledev.tennis_tune.databinding.SettingsPageBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.title = "Settings" // Correct the title here

        // Set up the action when the button is pressed
        binding.buttonBack.setOnClickListener {
            finish() // Finish this activity and go back to the previous one
        }
    }
}
