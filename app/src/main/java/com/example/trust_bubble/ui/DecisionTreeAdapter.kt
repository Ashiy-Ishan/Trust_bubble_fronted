package com.example.trust_bubble.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trust_bubble.R
import com.example.trust_bubble.databinding.DecisionStepItemBinding

// A simple data class to hold the information for each step in the linear path
data class DecisionStep(val type: StepType, val text: String)
enum class StepType { QUESTION, REASONING, RESULT } // 'REASONING' replaces 'ANSWER'

class DecisionTreeAdapter(private val steps: List<DecisionStep>) :
    RecyclerView.Adapter<DecisionTreeAdapter.ViewHolder>() {

    class ViewHolder(val binding: DecisionStepItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DecisionStepItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]
        holder.binding.tvStepText.text = step.text

        // Set the correct icon for the step type
        when (step.type) {
            StepType.QUESTION -> holder.binding.ivStepIcon.setImageResource(R.drawable.ic_question)
            StepType.REASONING -> holder.binding.ivStepIcon.setImageResource(R.drawable.ic_reasoning) // Use a new 'reasoning' icon
            StepType.RESULT -> holder.binding.ivStepIcon.setImageResource(R.drawable.ic_result)
        }
    }

    override fun getItemCount() = steps.size
}