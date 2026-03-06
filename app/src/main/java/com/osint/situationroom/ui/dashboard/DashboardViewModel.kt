package com.osint.situationroom.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osint.situationroom.data.model.*
import com.osint.situationroom.data.repository.OsintRepository
import com.osint.situationroom.domain.ConflictZoneData
import com.osint.situationroom.domain.TrajectoryAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val assessment: GlobalThreatAssessment? = null,
    val topEvents: List<OsintEvent> = emptyList(),
    val conflictZones: List<ConflictZone> = emptyList(),
    val regionalAlerts: List<RegionalAlert> = emptyList(),
    val trajectoryPoints: List<TrajectoryPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastRefresh: Long = 0L
)

class DashboardViewModel : ViewModel() {

    private val repository = OsintRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
        startAutoRefresh()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load static conflict zones immediately (instant)
            val zones = repository.getConflictZones()
            val alerts = repository.getRegionalAlerts()
            _uiState.value = _uiState.value.copy(conflictZones = zones, regionalAlerts = alerts)

            // Fetch live assessment
            val assessmentResult = repository.getGlobalAssessment()
            assessmentResult.onSuccess { assessment ->
                _uiState.value = _uiState.value.copy(assessment = assessment)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "Live data unavailable: ${e.message?.take(60)}")
            }

            // Fetch live events
            val eventsResult = repository.getOsintEvents()
            eventsResult.onSuccess { events ->
                _uiState.value = _uiState.value.copy(topEvents = events.take(10))
            }

            // Fetch trajectory
            val trajectoryResult = repository.getTrajectoryTimeline()
            trajectoryResult.onSuccess { points ->
                _uiState.value = _uiState.value.copy(trajectoryPoints = points)
            }.onFailure {
                _uiState.value = _uiState.value.copy(trajectoryPoints = TrajectoryAnalyzer.syntheticBaseline())
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                lastRefresh = System.currentTimeMillis()
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            load()
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // every 5 minutes
                if (!_uiState.value.isLoading) refresh()
            }
        }
    }
}
