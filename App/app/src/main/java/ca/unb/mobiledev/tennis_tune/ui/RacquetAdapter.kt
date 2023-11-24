import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.R
import ca.unb.mobiledev.tennis_tune.entity.Racquet

class RacquetAdapter(private val onClick: (Racquet) -> Unit) :
    ListAdapter<Racquet, RacquetAdapter.RacquetViewHolder>(RacquetDiffCallback()) {

    class RacquetViewHolder(itemView: View, val onClick: (Racquet) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val racquetNameTextView: TextView =
            itemView.findViewById(R.id.racquet_list_item_racquet_name)
        private var currentRacquet: Racquet? = null

        init {
            itemView.setOnClickListener {
                currentRacquet?.let(onClick)
            }
        }

        fun bind(racquet: Racquet) {
            currentRacquet = racquet
            racquetNameTextView.text = racquet.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RacquetViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.racquet_list_item, parent, false)
        return RacquetViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: RacquetViewHolder, position: Int) {
        holder.bind(getItem(position))
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