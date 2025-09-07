package com.example.trust_bubble.data // Or your correct package name

import com.google.gson.annotations.SerializedName

// Renamed 'decision_tree' to 'decision_path' to match the new prompt
data class AnalysisResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("classification") val classification: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("decision_path") val decisionPath: DecisionNode
)

data class HistoryResponse(
    @SerializedName("history") val history: List<AnalysisResponse>
)

// New structure to include the 'reason'
data class DecisionNode(
    @SerializedName("question") val question: String?,
    @SerializedName("decision") val decision: String?,
    @SerializedName("yes") val yes: DecisionBranch?,
    @SerializedName("no") val no: DecisionBranch?
)

data class DecisionBranch(
    @SerializedName("reason") val reason: String?,
    @SerializedName("next_step") val nextStep: DecisionNode?
)