package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
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
    private var currentRacquets: List<Racquet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RacquetListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarSettings)
        supportActionBar?.title = getString(R.string.racquets)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val fab: FloatingActionButton = binding.fabAddRacquet
        fab.setOnClickListener {
            val intent = Intent(this@RacquetListActivity, AddEditRacquetActivity::class.java)
            startActivity(intent)
        }

        setupRecyclerView()

        viewModel = ViewModelProvider(
            this,
            RacquetViewModelFactory(application)
        )[RacquetViewModel::class.java]

        viewModel.allRacquets.observe(this) { racquets ->
            racquets?.let {
                currentRacquets = it
                adapter.submitList(it)

                val sharedPreferences =
                    getSharedPreferences("AppSettings", MODE_PRIVATE)
                val selectedRacquetId = sharedPreferences.getInt("SELECTED_RACQUET_ID", -1)

                if (selectedRacquetId == -1 && it.size == 1) {
                    val defaultRacquetId = it.first().id
                    saveSelectedRacquetId(
                        application.applicationContext,
                        defaultRacquetId
                    )
                    adapter.setSelectedRacquetById(defaultRacquetId)
                } else if (selectedRacquetId != -1) {
                    adapter.setSelectedRacquetById(selectedRacquetId)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.racquets_recycler_view)
        adapter = RacquetAdapter(this) { racquet ->
            saveSelectedRacquetId(this, racquet.id)
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
                return false // Not moving items up/down
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val racquetToDelete = adapter.currentList[position]

                if (currentRacquets.size > 1) {
                    AlertDialog.Builder(this@RacquetListActivity)
                        .setTitle("Delete Racquet")
                        .setMessage("Are you sure you want to delete this racquet?")
                        .setPositiveButton("Delete") { dialog, which ->
                            viewModel.deleteRacquet(racquetToDelete)
                            adapter.selectNextAfterDeletion(racquetToDelete)
                        }
                        .setNegativeButton("Cancel") { dialog, which ->
                            adapter.notifyItemChanged(position) // Revert the swipe
                        }
                        .setOnCancelListener {
                            adapter.notifyItemChanged(position) // Revert the swipe if cancelled
                        }
                        .show()
                } else {
                    Toast.makeText(
                        this@RacquetListActivity,
                        "Cannot delete the last racquet",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter.notifyItemChanged(position) // Undo the swipe
                }
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

    companion object {
        fun saveSelectedRacquetId(context: Context, selectedRacquetId: Int) {
            val sharedPreferences =
                context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt("SELECTED_RACQUET_ID", selectedRacquetId)
                apply()
            }
        }
    }
}