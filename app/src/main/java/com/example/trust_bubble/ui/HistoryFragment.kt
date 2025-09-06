package com.example.trust_bubble.ui // Or your correct package name

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.example.trust_bubble.databinding.FragmentHistoryBinding
import com.example.trust_bubble.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // In ui/HistoryFragment.kt

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        // Set the click listener for the new button
        binding.btnClearHistory.setOnClickListener {
            clearHistory()
        }
    }

    // Add this new function to your HistoryFragment class
    private fun clearHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.clearHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "History cleared!", Toast.LENGTH_SHORT).show()
                        // Refresh the list, which will now be empty
                        fetchHistory()
                    } else {
                        Toast.makeText(requireContext(), "Failed to clear history.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // Fetch a fresh list every time the screen is shown
        fetchHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { analysisItem ->
            val intent = Intent(requireContext(), DecisionTreeActivity::class.java).apply {
                // Pass the decision tree as a JSON string to the next activity
                putExtra("decision_tree_json", Gson().toJson(analysisItem.decisionTree))
            }
            startActivity(intent)
        }
        binding.historyRecyclerView.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun fetchHistory() {
        // Show a loading state if you have one, or just wait
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val historyList = response.body()?.history ?: emptyList()
                        // This is the key line: submitList REPLACES the old list
                        historyAdapter.submitList(historyList)
                        binding.emptyView.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.emptyView.text = "Failed to load history"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.emptyView.text = "Error: ${e.message}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}