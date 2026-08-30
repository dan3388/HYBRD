package com.example.hibreed

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.hibreed.data.Exercise
import com.example.hibreed.databinding.FragmentExercisePickerBinding
import com.example.hibreed.ui.ExerciseListItem
import com.example.hibreed.ui.ExercisePickerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class ExercisePickerFragment : Fragment() {

    private var _binding: FragmentExercisePickerBinding? = null
    private val binding get() = _binding!!

    private val app get() = requireActivity().application as HibreedApp

    private lateinit var adapter: ExercisePickerAdapter
    private var allExercises: List<Exercise> = emptyList()
    private var query: String = ""

    private val resultKey = "exercisePickerResult"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExercisePickerAdapter(
            onExerciseClick = { exercise -> deliverExercise(exercise) }
        )
        binding.exerciseList.adapter = adapter

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                render()
            }
        })

        binding.addCustomBtn.setOnClickListener { showAddCustomDialog() }

        loadExercises()
    }

    private fun loadExercises() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = app.exerciseDao.getAll()
            allExercises = list
            render()
        }
    }

    private fun render() {
        val filtered = if (query.isEmpty()) {
            allExercises
        } else {
            val q = query.lowercase(Locale.getDefault())
            allExercises.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                    it.muscleGroup.lowercase(Locale.getDefault()).contains(q)
            }
        }

        val grouped = linkedMapOf<String, MutableList<Exercise>>()
        // stable ordering: alphabetical by muscle group, then by name
        filtered
            .sortedWith(compareBy({ it.muscleGroup }, { it.name }))
            .forEach { grouped.getOrPut(it.muscleGroup) { mutableListOf() }.add(it) }

        val items = mutableListOf<ExerciseListItem>()
        for ((group, exercises) in grouped) {
            items.add(ExerciseListItem.GroupHeader(group))
            exercises.forEach { items.add(ExerciseListItem.ExerciseRow(it)) }
        }

        binding.pickerEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        adapter.submit(items)
    }

    private fun deliverExercise(exercise: Exercise) {
        parentFragmentManager.setFragmentResult(
            resultKey,
            Bundle().apply {
                putLong("exerciseId", exercise.id)
                putString("exerciseName", exercise.name)
                putString("exerciseGroup", exercise.muscleGroup)
            }
        )
        findNavController().navigateUp()
    }

    private fun showAddCustomDialog() {
        val groups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Core", "Full Body")
        val content = layoutInflater.inflate(R.layout.dialog_add_exercise, null)
        val nameInput = content.findViewById<EditText>(R.id.customNameInput)
        val groupSpinner = content.findViewById<TextView>(R.id.customGroupSpinner)

        // Simple group selector using a MaterialAlertDialog with a single-choice
        var chosenGroup = groups[0]
        content.findViewById<View>(R.id.customGroupSpinner).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.custom_group_label))
                .setItems(groups.toTypedArray()) { _, which ->
                    chosenGroup = groups[which]
                    (groupSpinner as? android.widget.TextView)?.text = chosenGroup
                }
                .show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.custom_dialog_title))
            .setView(content)
            .setPositiveButton(getString(R.string.custom_add)) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Exercise name can't be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val existing = app.exerciseDao.findByName(name)
                    val exercise = if (existing != null) {
                        existing
                    } else {
                        val id = app.exerciseDao.insert(
                            Exercise(name = name, muscleGroup = chosenGroup, isCustom = true)
                        )
                        app.exerciseDao.findByName(name) ?: Exercise(id, name, chosenGroup, true)
                    }
                    deliverExercise(exercise)
                }
            }
            .setNegativeButton(getString(R.string.custom_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
