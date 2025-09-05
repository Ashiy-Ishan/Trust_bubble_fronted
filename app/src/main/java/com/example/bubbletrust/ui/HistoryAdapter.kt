package com.yourdomain.bubbletrust.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourdomain.bubbletrust.data.AnalysisResponse
import com.yourdomain.bubbletrust.databinding.HistoryItemBinding

class HistoryAdapter(private val onItemClicked: (AnalysisResponse) -> Unit) :
    ListAdapter<AnalysisResponse, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener { onItemClicked(current) }
        holder.bind(current)
    }

    class HistoryViewHolder(private var binding: HistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(analysis: AnalysisResponse) {
            binding.tvClassification.text = "Classification: ${analysis.classification}"
            binding.tvSummary.text = analysis.summary
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AnalysisResponse>() {
            override fun areItemsTheSame(oldItem: AnalysisResponse, newItem: AnalysisResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: AnalysisResponse, newItem: AnalysisResponse): Boolean {
                return oldItem == newItem
            }
        }
    }
}