package com.yourdomain.bubbletrust.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("SELECT * FROM analysis_history ORDER BY id DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>
}