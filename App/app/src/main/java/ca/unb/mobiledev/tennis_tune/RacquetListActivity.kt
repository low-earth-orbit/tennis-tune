package ca.unb.mobiledev.tennis_tune

import RacquetAdapter
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RacquetListActivity : AppCompatActivity() {

    private lateinit var adapter: RacquetAdapter
    private val viewModel: RacquetViewModel by viewModels {
        RacquetViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.racquet_list)

        val recyclerView: RecyclerView = findViewById(R.id.racquets_recycler_view)
        viewModel.allRacquets?.observe(this) { racquets ->
            racquets?.let {
                adapter = RacquetAdapter(it) { racquet -> adapterOnClick(racquet) }
                recyclerView.adapter = adapter
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        val fab: FloatingActionButton = findViewById(R.id.fab_add_racquet)
        fab.setOnClickListener {
            // Navigate to Add/Edit Activity
//            val intent = Intent(this, AddEditRacquetActivity::class.java)
//            startActivity(intent)
        }
    }

    private fun adapterOnClick(racquet: Racquet) {
        // Handle the racquet item click here
//        val intent = Intent(this, AddEditRacquetActivity::class.java)
//        intent.putExtra(RACQUET_ID, racquet.id)
//        startActivityForResult(intent, EDIT_RACQUET_REQUEST_CODE)
    }

    companion object {
        const val ADD_RACQUET_REQUEST_CODE = 1
        const val EDIT_RACQUET_REQUEST_CODE = 2
        const val RACQUET_ID = "racquet_id"
    }
}