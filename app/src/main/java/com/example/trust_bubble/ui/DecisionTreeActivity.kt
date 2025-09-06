package com.example.trust_bubble.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.example.trust_bubble.data.DecisionNode
import com.example.trust_bubble.databinding.ActivityDecisionTreeBinding

class DecisionTreeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecisionTreeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecisionTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.treeContainer.removeAllViews() // Clear old text-based views
        val recyclerView = androidx.recyclerview.widget.RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        binding.treeContainer.addView(recyclerView)

        val jsonString = intent.getStringExtra("decision_tree_json")
        if (jsonString != null) {
            try {
                val decisionTree = Gson().fromJson(jsonString, DecisionNode::class.java)
                // Convert the nested tree into a list of DecisionPoint cards
                val decisionPath = buildDecisionPath(decisionTree)
                recyclerView.adapter = DecisionTreeAdapter(decisionPath)
            } catch (e: Exception) {
                // Handle error if JSON is malformed
            }
        }
    }

    // This function walks the AI's path and builds a list of cards to display
    private fun buildDecisionPath(rootNode: DecisionNode): List<DecisionPoint> {
        val path = mutableListOf<DecisionPoint>()
        var currentNode: DecisionNode? = rootNode

        while (currentNode?.decision == null && currentNode?.question != null) {
            val question = currentNode.question
            val yesNode = currentNode.yes
            val noNode = currentNode.no

            // Determine the text for the yes/no outcomes
            val yesText = yesNode?.decision ?: yesNode?.question ?: "Error"
            val noText = noNode?.decision ?: noNode?.question ?: "Error"

            // Find which path was taken to determine the next node
            // This is a simplified way to determine the path from the JSON structure
            val nextNode: DecisionNode?
            val pathTaken: Path

            // A simple heuristic: assume the path with a non-null 'decision' or 'question' is the one taken next.
            // This works for a linear path extracted from the tree.
            if (yesNode != null && (yesNode.decision != null || yesNode.question != null) && (noNode?.decision == null && noNode?.question == null)) {
                pathTaken = Path.YES
                nextNode = yesNode
            } else if (noNode != null && (noNode.decision != null || noNode.question != null) && (yesNode?.decision == null && yesNode?.question == null)) {
                pathTaken = Path.NO
                nextNode = noNode
            } else {
                // This logic needs to be smarter if the tree is more complex, but for a single path it's okay.
                // We'll just assume one branch leads to a dead end.
                if (yesNode != null) {
                    pathTaken = Path.YES
                    nextNode = yesNode
                } else {
                    pathTaken = Path.NO
                    nextNode = noNode
                }

            }

            path.add(DecisionPoint(question, yesText, noText, pathTaken))
            currentNode = nextNode
        }
        return path
    }
}