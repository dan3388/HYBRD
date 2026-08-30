package com.example.hibreed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hibreed.R

class SessionAdapter(
    private var items: List<SessionListItem> = emptyList(),
    private val onWorkoutClick: (SessionListItem.Workout) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_WORKOUT = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is SessionListItem.DayHeader) TYPE_HEADER else TYPE_WORKOUT

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_day_header, parent, false))
        } else {
            WorkoutViewHolder(inflater.inflate(R.layout.item_session, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SessionListItem.DayHeader -> (holder as HeaderViewHolder).bind(item)
            is SessionListItem.Workout -> (holder as WorkoutViewHolder).bind(item)
        }
    }

    fun submit(newItems: List<SessionListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.dayHeaderText)
        fun bind(item: SessionListItem.DayHeader) {
            text.text = item.displayDate
        }
    }

    inner class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.sessionExerciseName)
        private val details: TextView = view.findViewById(R.id.sessionDetails)

        init {
            itemView.setOnClickListener {
                val item = items.getOrNull(bindingAdapterPosition) as? SessionListItem.Workout
                if (item != null) onWorkoutClick(item)
            }
        }

        fun bind(item: SessionListItem.Workout) {
            val w = item.workout
            name.text = w.exercise.name
            val custom = if (w.exercise.isCustom) " · custom" else ""
            val titlePrefix = "${w.exercise.muscleGroup}$custom"
            val setLines = if (w.sets.isEmpty()) {
                "No sets"
            } else {
                w.sets.sortedBy { it.sortOrder }
                    .joinToString("  ") { set ->
                        val load = if (set.weightLbs > 0) formatWeight(set.weightLbs) else "bw"
                        "$load × ${set.reps}"
                    }
            }
            details.text = "$titlePrefix\n$setLines"
        }
    }

    private fun formatWeight(w: Double): String =
        if (w == Math.floor(w) && !w.isInfinite()) w.toLong().toString() else w.toString()
}
