package com.yourdomain.bubbletrust.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.yourdomain.bubbletrust.R
import com.yourdomain.bubbletrust.data.DecisionNode
import com.yourdomain.bubbletrust.databinding.ActivityDecisionTreeBinding

class DecisionTreeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecisionTreeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecisionTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonString = intent.getStringExtra("decision_tree_json")
        if (jsonString != null) {
            val decisionTree = Gson().fromJson(jsonString, DecisionNode::class.java)
            renderTreeView(decisionTree, binding.treeContainer, 0)
        }
    }

    private fun renderTreeView(node: DecisionNode, container: ViewGroup, depth: Int) {
        val indent = (depth * 40).toFloat() // 40 pixels of indentation per level

        if (node.decision != null) {
            val decisionView = createTextView("✅ Result: ${node.decision}", isBold = true)
            decisionView.setPadding(indent.toInt(), 16, 0, 16)
            container.addView(decisionView)
        } else if (node.question != null) {
            val questionView = createTextView("❓ ${node.question}")
            questionView.setPadding(indent.toInt(), 16, 0, 16)
            container.addView(questionView)

            node.yes?.let {
                val yesPrefix = createTextView("    YES ➜", isBold = true)
                yesPrefix.setPadding(indent.toInt(), 8, 0, 0)
                yesPrefix.setTextColor(ContextCompat.getColor(this, R.color.yes_color_green))
                container.addView(yesPrefix)
                renderTreeView(it, container, depth + 1)
            }
            node.no?.let {
                val noPrefix = createTextView("    NO ➜", isBold = true)
                noPrefix.setPadding(indent.toInt(), 8, 0, 0)
                noPrefix.setTextColor(ContextCompat.getColor(this, R.color.no_color_red))
                container.addView(noPrefix)
                renderTreeView(it, container, depth + 1)
            }
        }
    }

    private fun createTextView(text: String, isBold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            this.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.textSize = 16f
            if (isBold) {
                this.setTypeface(null, Typeface.BOLD)
            }
        }
    }
}