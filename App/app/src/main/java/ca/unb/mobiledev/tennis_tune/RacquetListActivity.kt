package ca.unb.mobiledev.tennis_tune

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.databinding.RacquetListBinding
import ca.unb.mobiledev.tennis_tune.entity.Racquet
import ca.unb.mobiledev.tennis_tune.ui.RacquetAdapter
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModel
import ca.unb.mobiledev.tennis_tune.ui.RacquetViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RacquetListActivity : AppCompatActivity() {
    private lateinit var binding: RacquetListBinding
    private lateinit var adapter: RacquetAdapter
    private lateinit var viewModel: RacquetViewModel
    private var selectedRacquetId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RacquetListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = getString(R.string.racquets)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        viewModel = ViewModelProvider(
            this,
            RacquetViewModelFactory(application)
        )[RacquetViewModel::class.java]

        setupRecyclerView()

        viewModel.allRacquets.observe(this) { racquets ->
            racquets?.let { adapter.submitList(it) }
        }

        val fab: FloatingActionButton = binding.fabAddRacquet
        fab.setOnClickListener {
            val intent = Intent(this@RacquetListActivity, AddEditRacquetActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.racquets_recycler_view)
        adapter = RacquetAdapter { racquet ->
            selectedRacquetId = racquet.id
            // TODO
            // Persist the selection in SharedPreferences
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We are not moving items up/down
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val racquetToDelete = adapter.currentList[position]

                AlertDialog.Builder(this@RacquetListActivity)
                    .setTitle("Delete Racquet")
                    .setMessage("Are you sure you want to delete this racquet?")
                    .setPositiveButton("Delete") { dialog, which ->
                        viewModel.delete(racquetToDelete)
                    }
                    .setNegativeButton("Cancel") { dialog, which ->
                        adapter.notifyItemChanged(position) // Revert the swipe
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position) // Revert the swipe if cancelled
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
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