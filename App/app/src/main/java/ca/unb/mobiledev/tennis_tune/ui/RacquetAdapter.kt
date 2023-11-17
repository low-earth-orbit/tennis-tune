import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.unb.mobiledev.tennis_tune.R
import ca.unb.mobiledev.tennis_tune.entity.Racquet


class RacquetAdapter(private val items: List<Racquet>, private val onClick: (Racquet) -> Unit) :
    RecyclerView.Adapter<RacquetAdapter.RacquetViewHolder>() {

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
        val racquet = items[position]
        holder.bind(racquet)
    }

    override fun getItemCount(): Int = items.size
}