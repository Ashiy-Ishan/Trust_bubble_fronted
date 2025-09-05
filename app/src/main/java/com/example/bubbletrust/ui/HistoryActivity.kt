package com.yourdomain.bubbletrust.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.yourdomain.bubbletrust.data.AnalysisResponse
import com.yourdomain.bubbletrust.databinding.ActivityHistoryBinding
import com.yourdomain.bubbletrust.network.RetrofitClient
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        fetchHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { analysis ->
            val intent = Intent(this, DecisionTreeActivity::class.java)
            // Pass the decision tree as a JSON string
            intent.putExtra("decision_tree_json", Gson().toJson(analysis.decisionTree))
            startActivity(intent)
        }
        binding.historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }

    private fun fetchHistory() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getHistory()
                if (response.isSuccessful) {
                    response.body()?.history?.let {
                        historyAdapter.submitList(it)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}