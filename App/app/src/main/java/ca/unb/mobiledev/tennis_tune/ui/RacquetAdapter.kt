package ca.unb.mobiledev.tennis_tune.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.AddEditRacquetActivity
import ca.unb.mobiledev.tennis_tune.R
import ca.unb.mobiledev.tennis_tune.RacquetListActivity
import ca.unb.mobiledev.tennis_tune.entity.Racquet

class RacquetAdapter(
    private val context: Context,
    private val viewModel: RacquetViewModel,
    private val onClick: (Racquet) -> Unit
) : ListAdapter<Racquet, RacquetAdapter.RacquetViewHolder>(RacquetDiffCallback()) {
    private var selectedRacquet: Racquet? = null

    inner class RacquetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deleteButton = itemView.findViewById<ImageView>(R.id.deleteButton)
        private val editButton = itemView.findViewById<ImageView>(R.id.editButton)
        private val racquetNameTextView: TextView =
            itemView.findViewById(R.id.racquet_list_item_racquet_name)
        private val racquetListItemCheck: ImageView =
            itemView.findViewById(R.id.racquet_list_item_check)
        private val racquetListItem: FrameLayout = itemView.findViewById(R.id.racquetListItem)

        init {
            // Delete racquet
            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val racquetToDelete = getItem(position)
                    if (currentList.size > 1) {
                        AlertDialog.Builder(context)
                            .setTitle("Delete Racquet")
                            .setMessage(
                                "This cannot be undone. Are you sure you want to delete " +
                                        "this racquet?"
                            )
                            .setPositiveButton("Delete") { _, _ ->
                                // Delete operation
                                viewModel.deleteRacquet(racquetToDelete)
                                // Select the next
                                selectNextAfterDeletion(racquetToDelete)
                                // Update SharedPreferences
                                getSelectedRacquet()?.id?.let {
                                    RacquetListActivity.saveSelectedRacquetId(context, it)
                                }
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                            }
                            .setOnCancelListener {
                            }
                            .show()
                    } else {
                        Toast.makeText(
                            context,
                            "Cannot delete the last racquet",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Edit racquet
            editButton.setOnClickListener() {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val racquetToEdit = getItem(position)
                    val intent = Intent(context, AddEditRacquetActivity::class.java).apply {
                        putExtra("EXTRA_RACQUET_ID", racquetToEdit.id)
                    }
                    context.startActivity(intent)
                }
            }

            // Select racquet
            racquetListItem.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val newSelectedRacquet = getItem(position)
                    if (selectedRacquet != newSelectedRacquet) {
                        selectedRacquet?.let { oldRacquet ->
                            notifyItemChanged(currentList.indexOf(oldRacquet))
                        }
                        selectedRacquet = newSelectedRacquet
                        notifyItemChanged(position)
                        onClick(newSelectedRacquet)
                    }
                }
            }
        }

        fun bind(racquet: Racquet) {
            // Display racquet name
            racquetNameTextView.text = racquet.name

            // Set check mark visibility
            if (selectedRacquet == null) {
                racquetListItemCheck.isVisible = true // Pre-check the default racquet
            } else {
                racquetListItemCheck.isVisible = racquet == selectedRacquet
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RacquetViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.racquet_list_item, parent, false)
        return RacquetViewHolder(view)
    }

    override fun onBindViewHolder(holder: RacquetViewHolder, position: Int) {
        val racquet = getItem(position)
        holder.bind(racquet)
    }

    fun getSelectedRacquet(): Racquet? {
        return selectedRacquet
    }

    fun setSelectedRacquetById(selectedRacquetId: Int) {
        val currentList = currentList
        selectedRacquet = currentList.find { it.id == selectedRacquetId }
        notifyDataSetChanged()
    }

    fun selectNextAfterDeletion(deletedRacquet: Racquet) {
        val currentList = currentList
        val deletedIndex = currentList.indexOf(deletedRacquet)

        if (deletedRacquet == selectedRacquet && currentList.size > 1) {
            val newSelectedIndex = if (deletedIndex == 0) 1 else deletedIndex - 1
            selectedRacquet = currentList[newSelectedIndex]
        }
        notifyDataSetChanged()
    }

    class RacquetDiffCallback : DiffUtil.ItemCallback<Racquet>() {
        override fun areItemsTheSame(oldItem: Racquet, newItem: Racquet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Racquet, newItem: Racquet): Boolean {
            return oldItem == newItem
        }
    }
}