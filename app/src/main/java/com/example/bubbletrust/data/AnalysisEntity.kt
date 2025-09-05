package com.yourdomain.bubbletrust.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classification: String,
    val summary: String,
    val decisionTreeJson: String // Store the complex object as a JSON string
)