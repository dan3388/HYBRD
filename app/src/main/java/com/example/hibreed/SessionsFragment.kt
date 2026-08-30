package com.example.hibreed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hibreed.databinding.FragmentSessionsBinding
import com.example.hibreed.data.WorkoutWithEverything
import com.example.hibreed.ui.SessionAdapter
import com.example.hibreed.ui.SessionListItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionsFragment : Fragment() {

    private var _binding: FragmentSessionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SessionAdapter

    private val app get() = requireActivity().application as HibreedApp

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SessionAdapter(
            onWorkoutClick = { item ->
                // Open the editor for this workout.
                findNavController().navigate(
                    R.id.action_SessionsFragment_to_LogFragment,
                    android.os.Bundle().apply { putLong("workoutId", item.workout.id) }
                )
            }
        )
        binding.sessionsList.adapter = adapter
        binding.addWorkoutFab.setOnClickListener {
            findNavController().navigate(R.id.action_SessionsFragment_to_LogFragment)
        }

        loadSessions()
    }

    private fun loadSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            val workouts = app.workoutDao.getAllWithExercise()
            val items = groupByDay(workouts)
            binding.sessionsList.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            adapter.submit(items)
        }
    }

    private fun groupByDay(workouts: List<WorkoutWithEverything>): List<SessionListItem> {
        val groups = linkedMapOf<String, MutableList<WorkoutWithEverything>>()
        for (w in workouts) {
            groups.getOrPut(w.date) { mutableListOf() }.add(w)
        }
        val result = mutableListOf<SessionListItem>()
        for ((dateKey, dayWorkouts) in groups) {
            result.add(SessionListItem.DayHeader(dateKey, prettyDate(dateKey)))
            dayWorkouts.forEach { result.add(SessionListItem.Workout(it)) }
        }
        return result
    }

    private fun prettyDate(iso: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            val d: Date = inFmt.parse(iso) ?: return iso
            outFmt.format(shiftToLocal(d))
        } catch (e: Exception) {
            iso
        }
    }

    private fun shiftToLocal(d: Date): Date {
        val cal = java.util.Calendar.getInstance()
        cal.time = d
        val tz = cal.timeZone
        return Date(d.time + tz.getOffset(d.time))
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) loadSessions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
