package com.osint.situationroom.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osint.situationroom.data.model.ConflictZone
import com.osint.situationroom.data.model.RegionalAlert
import com.osint.situationroom.data.repository.OsintRepository
import com.osint.situationroom.domain.ConflictZoneData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val conflictZones: List<ConflictZone> = emptyList(),
    val selectedZone: ConflictZone? = null,
    val isLoading: Boolean = true
)

class MapViewModel : ViewModel() {

    private val repository = OsintRepository()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val zones = repository.getConflictZones()
            _uiState.value = MapUiState(conflictZones = zones, isLoading = false)
        }
    }

    fun selectZone(zone: ConflictZone?) {
        _uiState.value = _uiState.value.copy(selectedZone = zone)
    }
}
