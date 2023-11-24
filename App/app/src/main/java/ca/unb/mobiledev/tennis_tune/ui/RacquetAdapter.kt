package ca.unb.mobiledev.tennis_tune.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.R
import ca.unb.mobiledev.tennis_tune.entity.Racquet

class RacquetAdapter(private val onClick: (Racquet) -> Unit) :
    ListAdapter<Racquet, RacquetAdapter.RacquetViewHolder>(RacquetDiffCallback()) {
    private var selectedRacquet: Racquet? = null

    inner class RacquetViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val racquetNameTextView: TextView =
            itemView.findViewById(R.id.racquet_list_item_racquet_name)
        private val radioButtonSelect: RadioButton = itemView.findViewById(R.id.radioButtonSelect)

        init {
            itemView.setOnClickListener {
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
            racquetNameTextView.text = racquet.name
            radioButtonSelect.isChecked = racquet == selectedRacquet
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

    fun setSelectedRacquetById(selectedRacquetId: Int) {
        val currentList = currentList
        selectedRacquet = currentList.find { it.id == selectedRacquetId }
        notifyDataSetChanged()
    }

    fun selectNextAfterDeletion(deletedRacquet: Racquet) {
        val currentList = currentList
        val deletedIndex = currentList.indexOf(deletedRacquet)

        if (currentList.size > 1) {
            val newSelectedIndex = if (deletedIndex == 0) 1 else deletedIndex - 1
            selectedRacquet = currentList[newSelectedIndex]
            notifyItemRemoved(deletedIndex)
            notifyItemChanged(newSelectedIndex)
        } else {
            selectedRacquet = null
            notifyItemRemoved(deletedIndex)
        }
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