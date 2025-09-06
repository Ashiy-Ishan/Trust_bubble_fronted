package com.example.trust_bubble.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trust_bubble.data.AnalysisResponse
import com.example.trust_bubble.databinding.HistoryItemBinding

class HistoryAdapter(
    // This is a function that will be called when an item is clicked
    private val onItemClicked: (AnalysisResponse) -> Unit
) : ListAdapter<AnalysisResponse, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    // Creates a new ViewHolder when the RecyclerView needs one.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    // Binds the data to a ViewHolder at a specific position in the list.
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentItem = getItem(position)
        // Set the click listener for the entire row
        holder.itemView.setOnClickListener {
            onItemClicked(currentItem)
        }
        holder.bind(currentItem)
    }

    // Inner class to hold the views for a single list item.
    class HistoryViewHolder(private val binding: HistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(analysis: AnalysisResponse) {
            binding.tvClassification.text = "Classification: ${analysis.classification}"
            binding.tvSummary.text = analysis.summary
        }
    }

    // Companion object to efficiently calculate differences between two lists.
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AnalysisResponse>() {
            override fun areItemsTheSame(oldItem: AnalysisResponse, newItem: AnalysisResponse): Boolean {
                // An item is the "same" if it has the same unique ID.
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: AnalysisResponse, newItem: AnalysisResponse): Boolean {
                // The contents are the "same" if all the data is identical.
                // Since AnalysisResponse is a data class, '==' works perfectly.
                return oldItem == newItem
            }
        }
    }
}