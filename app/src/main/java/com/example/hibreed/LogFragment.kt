package com.example.hibreed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hibreed.data.Exercise
import com.example.hibreed.data.Set
import com.example.hibreed.databinding.FragmentLogBinding
import com.example.hibreed.databinding.ItemEditSetBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as HibreedApp

    private var selectedExercise: Exercise? = null
    private var workoutId: Long = 0
    private var selectedDateMillis: Long = System.currentTimeMillis()

    private val resultKey = "exercisePickerResult"

    // Each row is (rootView, weightInput, repsInput, indexView)
    private class SetRow(val root: View, val weight: EditText, val reps: EditText, val index: TextView)

    private val rows = mutableListOf<SetRow>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workoutId = arguments?.getLong("workoutId") ?: 0L

        setFragmentResultListener(resultKey) { _, bundle ->
            val id = bundle.getLong("exerciseId", -1L)
            val name = bundle.getString("exerciseName")
            if (id > 0 && name != null) {
                selectedExercise = Exercise(
                    id = id,
                    name = name,
                    muscleGroup = bundle.getString("exerciseGroup").orEmpty()
                )
                binding.exerciseName.setText(name)
            }
        }

        binding.pickExerciseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_LogFragment_to_ExercisePickerFragment)
        }
        binding.dateButton.setOnClickListener { showDatePicker() }
        binding.addSetBtn.setOnClickListener { addSetRowInternal() }
        binding.saveWorkoutBtn.setOnClickListener { save() }
        binding.deleteWorkoutBtn.setOnClickListener { confirmDelete() }

        // Reset editor state so re-entry (e.g. round-tripping through the exercise
        // picker re-uses this same fragment instance) always starts clean.
        rows.clear()
        binding.setsContainer.removeAllViews()

        if (workoutId > 0) {
            binding.deleteWorkoutBtn.visibility = View.VISIBLE
            loadWorkoutForEditing()
        } else {
            addSetRowInternal()
        }
    }

    private fun loadWorkoutForEditing() {
        viewLifecycleOwner.lifecycleScope.launch {
            val w = app.workoutDao.getWorkoutWithSets(workoutId)
            if (w == null) {
                Toast.makeText(requireContext(), "Workout not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@launch
            }
            selectedExercise = w.exercise
            binding.exerciseName.setText(w.exercise.name)

            val cal = java.util.Calendar.getInstance()
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(w.date)
                cal.time = d ?: Date()
                updateDateLabel(cal.timeInMillis)
            } catch (e: Exception) {
                updateDateLabel(System.currentTimeMillis())
            }

            // Represent dates in local time so the picked millis maps back correctly.
            selectedDateMillis = localToUtcMillis(cal)

            rows.clear()
            binding.setsContainer.removeAllViews()
            if (w.sets.isEmpty()) {
                addSetRowInternal()
            } else {
                w.sets.sortedBy { it.sortOrder }.forEach { s ->
                    addSetRowInternal().also { row ->
                        row.weight.setText(formatNum(s.weightLbs))
                        row.reps.setText(s.reps.toString())
                    }
                }
            }
            updateSetIndices()
        }
    }

    private fun localToUtcMillis(cal: java.util.Calendar): Long {
        val tz = cal.timeZone
        return cal.timeInMillis - tz.getOffset(cal.timeInMillis)
    }

    private fun addSetRowInternal(): SetRow {
        val item = ItemEditSetBinding.inflate(layoutInflater, binding.setsContainer, false)
        binding.setsContainer.addView(item.root)

        val row = SetRow(item.root, item.setWeight, item.setReps, item.setIndex)
        item.removeSet.setOnClickListener { removeRow(row) }
        rows.add(row)
        updateSetIndices()
        return row
    }

    private fun removeRow(row: SetRow) {
        if (rows.size <= 1) {
            Toast.makeText(requireContext(), getString(R.string.log_error_no_sets), Toast.LENGTH_SHORT).show()
            return
        }
        rows.remove(row)
        binding.setsContainer.removeView(row.root)
        updateSetIndices()
    }

    private fun updateSetIndices() {
        rows.forEachIndexed { i, row -> row.index.text = (i+1).toString() }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selectedDateMillis)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDateMillis = millis
            updateDateLabel(millis)
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    private fun updateDateLabel(millis: Long) {
        val fmt = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        binding.dateButton.text = fmt.format(Date(millis))
    }

    private fun save() {
        val exercise = selectedExercise
        if (exercise == null) {
            Toast.makeText(requireContext(), getString(R.string.log_error_exercise), Toast.LENGTH_SHORT).show()
            return
        }

        val parsedSets = mutableListOf<Pair<Double, Int>>()
        for (row in rows) {
            val weight = row.weight.text.toString().toDoubleOrNull() ?: 0.0
            val reps = row.reps.text.toString().toIntOrNull() ?: 0
            if (weight < 0 || reps <= 0) {
                Toast.makeText(requireContext(), getString(R.string.log_error_reps), Toast.LENGTH_SHORT).show()
                return
            }
            parsedSets.add(weight to reps)
        }
        if (parsedSets.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.log_error_no_sets), Toast.LENGTH_SHORT).show()
            return
        }

        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(selectedDateMillis))

        viewLifecycleOwner.lifecycleScope.launch {
            if (workoutId > 0) {
                app.workoutDao.replaceSets(
                    workoutId,
                    parsedSets.map { Set(workoutId = workoutId, weightLbs = it.first, reps = it.second, sortOrder = 0) }
                )
            } else {
                val newId = app.workoutDao.insertWorkout(
                    com.example.hibreed.data.Workout(exerciseId = exercise.id, date = isoDate)
                )
                app.workoutDao.replaceSets(
                    newId,
                    parsedSets.map { Set(workoutId = newId, weightLbs = it.first, reps = it.second, sortOrder = 0) }
                )
            }
            Toast.makeText(requireContext(), getString(R.string.log_saved), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_workout))
            .setMessage("Delete this workout and all its sets?")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    app.workoutDao.deleteWorkout(workoutId)
                    Toast.makeText(requireContext(), getString(R.string.workout_deleted), Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatNum(d: Double): String =
        if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString() else d.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
