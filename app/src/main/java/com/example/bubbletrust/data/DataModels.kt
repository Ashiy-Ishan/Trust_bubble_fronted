package com.yourdomain.bubbletrust.data

import com.google.gson.annotations.SerializedName

// This model is used for network responses and UI display
data class AnalysisResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("classification") val classification: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("decision_tree") val decisionTree: DecisionNode
)

data class HistoryResponse(
    @SerializedName("history") val history: List<AnalysisResponse>
)

data class DecisionNode(
    @SerializedName("question") val question: String?,
    @SerializedName("decision") val decision: String?,
    @SerializedName("yes") val yes: DecisionNode?,
    @SerializedName("no") val no: DecisionNode?
)