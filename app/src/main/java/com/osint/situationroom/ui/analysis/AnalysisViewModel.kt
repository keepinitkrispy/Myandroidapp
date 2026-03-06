package com.osint.situationroom.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osint.situationroom.data.model.*
import com.osint.situationroom.data.repository.OsintRepository
import com.osint.situationroom.domain.ConflictZoneData
import com.osint.situationroom.domain.TrajectoryAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val trajectoryPoints: List<TrajectoryPoint> = emptyList(),
    val currentTrajectory: Trajectory = Trajectory.VOLATILE,
    val weeklyChange: Float = 0f,
    val conflictZones: List<ConflictZone> = emptyList(),
    val assessment: GlobalThreatAssessment? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AnalysisViewModel : ViewModel() {

    private val repository = OsintRepository()
    private val _state = MutableStateFlow(AnalysisUiState())
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val zones = repository.getConflictZones()
            _state.value = _state.value.copy(conflictZones = zones)

            val trajectoryResult = repository.getTrajectoryTimeline()
            val points = trajectoryResult.getOrElse { TrajectoryAnalyzer.syntheticBaseline() }
            val currentTraj = TrajectoryAnalyzer.computeCurrentTrajectory(points)
            val change = TrajectoryAnalyzer.computeWeeklyChange(points)

            val assessmentResult = repository.getGlobalAssessment()
            val assessment = assessmentResult.getOrNull()

            _state.value = _state.value.copy(
                trajectoryPoints = points,
                currentTrajectory = currentTraj,
                weeklyChange = change,
                assessment = assessment,
                isLoading = false
            )
        }
    }
}
