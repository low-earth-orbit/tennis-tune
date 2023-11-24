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

    private var selectedPos = RecyclerView.NO_POSITION

    inner class RacquetViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val racquetNameTextView: TextView =
            itemView.findViewById(R.id.racquet_list_item_racquet_name)
        private val radioButtonSelect: RadioButton = itemView.findViewById(R.id.radioButtonSelect)

        init {
            itemView.setOnClickListener {
                notifyItemChanged(selectedPos)
                selectedPos = adapterPosition
                notifyItemChanged(selectedPos)
                onClick(getItem(adapterPosition))
            }
        }

        fun bind(racquet: Racquet, isSelected: Boolean) {
            racquetNameTextView.text = racquet.name
            radioButtonSelect.isChecked = isSelected
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RacquetViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.racquet_list_item, parent, false)
        return RacquetViewHolder(view)
    }

    override fun onBindViewHolder(holder: RacquetViewHolder, position: Int) {
        val racquet = getItem(position)
        holder.bind(racquet, selectedPos == position)

        holder.itemView.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.layoutPosition
            notifyItemChanged(selectedPos)
            onClick(racquet)
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