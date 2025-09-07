package com.example.trust_bubble.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.example.trust_bubble.data.DecisionNode
import com.example.trust_bubble.databinding.ActivityDecisionTreeBinding

class DecisionTreeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecisionTreeBinding
    private val decisionSteps = mutableListOf<DecisionStep>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecisionTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the JSON string passed from the previous activity
        val jsonString = intent.getStringExtra("decision_tree_json")

        if (jsonString != null) {
            try {
                // Use Gson to convert the JSON string back into a DecisionNode object
                val decisionTree = Gson().fromJson(jsonString, DecisionNode::class.java)

                // Convert the nested tree structure into a simple list of steps
                buildLinearPath(decisionTree)

                // Set up the RecyclerView to display the linear list of steps
                binding.decisionTreeRecyclerView.apply {
                    adapter = DecisionTreeAdapter(decisionSteps)
                    layoutManager = LinearLayoutManager(this@DecisionTreeActivity)
                }

            } catch (e: Exception) {
                // Handle parsing errors gracefully
                // For example, you could display a Toast or a message to the user
            }
        }
    }

    /**
     * This function recursively traverses the AI's decision path,
     * converting the nested tree into a flat, linear list of steps.
     * @param node The current decision node to process.
     */
    private fun buildLinearPath(node: DecisionNode?) {
        if (node == null) return

        // If the node has a final decision, add it to the list and stop
        if (node.decision != null) {
            decisionSteps.add(DecisionStep(StepType.RESULT, node.decision))
            return
        }

        // If the node has a question, add it to the list
        if (node.question != null) {
            decisionSteps.add(DecisionStep(StepType.QUESTION, node.question))

            val yesBranch = node.yes
            val noBranch = node.no

            // Check which path the AI took by seeing which 'reason' is not null
            if (yesBranch?.reason != null) {
                decisionSteps.add(DecisionStep(StepType.REASONING, yesBranch.reason))
                // Recursively call the function on the next step in the 'yes' branch
                buildLinearPath(yesBranch.nextStep)
            } else if (noBranch?.reason != null) {
                decisionSteps.add(DecisionStep(StepType.REASONING, noBranch.reason))
                // Recursively call the function on the next step in the 'no' branch
                buildLinearPath(noBranch.nextStep)
            }
        }
    }
}