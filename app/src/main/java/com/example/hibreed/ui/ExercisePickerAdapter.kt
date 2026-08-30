package com.example.hibreed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hibreed.R
import com.example.hibreed.data.Exercise

sealed class ExerciseListItem {
    data class GroupHeader(val group: String) : ExerciseListItem()
    data class ExerciseRow(val exercise: Exercise) : ExerciseListItem()
}

class ExercisePickerAdapter(
    private var items: List<ExerciseListItem> = emptyList(),
    private val onExerciseClick: (Exercise) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is ExerciseListItem.GroupHeader) TYPE_HEADER else TYPE_ROW

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_group_header, parent, false))
        } else {
            RowViewHolder(inflater.inflate(R.layout.item_exercise, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ExerciseListItem.GroupHeader -> (holder as HeaderViewHolder).bind(item)
            is ExerciseListItem.ExerciseRow -> (holder as RowViewHolder).bind(item)
        }
    }

    fun submit(newItems: List<ExerciseListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.groupHeaderText)
        fun bind(item: ExerciseListItem.GroupHeader) {
            text.text = item.group
        }
    }

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.exerciseName)

        init {
            itemView.setOnClickListener {
                val item = items.getOrNull(bindingAdapterPosition) as? ExerciseListItem.ExerciseRow
                if (item != null) onExerciseClick(item.exercise)
            }
        }

        fun bind(item: ExerciseListItem.ExerciseRow) {
            name.text = item.exercise.name
        }
    }
}
