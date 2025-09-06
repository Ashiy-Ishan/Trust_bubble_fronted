package com.example.trust_bubble.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.trust_bubble.R
import com.example.trust_bubble.databinding.DecisionCardItemBinding

// Data class to hold the info for one decision card
data class DecisionPoint(
    val question: String,
    val yesText: String,
    val noText: String,
    val pathTaken: Path
)
enum class Path { YES, NO, UNKNOWN }

class DecisionTreeAdapter(private val decisionPath: List<DecisionPoint>) :
    RecyclerView.Adapter<DecisionTreeAdapter.ViewHolder>() {

    class ViewHolder(val binding: DecisionCardItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DecisionCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val point = decisionPath[position]
        val context = holder.itemView.context

        holder.binding.tvQuestion.text = point.question
        holder.binding.tvYesText.text = point.yesText
        holder.binding.tvNoText.text = point.noText

        // Highlight the path that was taken and gray out the one that wasn't
        if (point.pathTaken == Path.YES) {
            // Highlight YES path
            holder.binding.tvYesLabel.setTextColor(ContextCompat.getColor(context, R.color.brand_purple))
            holder.binding.tvYesText.setTextColor(ContextCompat.getColor(context, R.color.text_color_primary))
            // Gray out NO path
            holder.binding.tvNoLabel.setTextColor(ContextCompat.getColor(context, R.color.text_color_secondary))
            holder.binding.tvNoText.setTextColor(ContextCompat.getColor(context, R.color.text_color_secondary))
        } else {
            // Highlight NO path
            holder.binding.tvNoLabel.setTextColor(ContextCompat.getColor(context, R.color.brand_purple))
            holder.binding.tvNoText.setTextColor(ContextCompat.getColor(context, R.color.text_color_primary))
            // Gray out YES path
            holder.binding.tvYesLabel.setTextColor(ContextCompat.getColor(context, R.color.text_color_secondary))
            holder.binding.tvYesText.setTextColor(ContextCompat.getColor(context, R.color.text_color_secondary))
        }
    }

    override fun getItemCount() = decisionPath.size
}