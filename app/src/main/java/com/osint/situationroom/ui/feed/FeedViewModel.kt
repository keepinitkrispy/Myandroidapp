package com.osint.situationroom.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osint.situationroom.data.model.EventType
import com.osint.situationroom.data.model.OsintEvent
import com.osint.situationroom.data.repository.OsintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val allEvents: List<OsintEvent> = emptyList(),
    val filteredEvents: List<OsintEvent> = emptyList(),
    val selectedType: EventType? = null,
    val selectedRegion: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

class FeedViewModel : ViewModel() {

    private val repository = OsintRepository()
    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init { loadFeed() }

    fun loadFeed() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.getOsintEvents(
                query = "war military nuclear missile attack ceasefire diplomacy troops NATO",
                timespan = "24h"
            )
            result.onSuccess { events ->
                _state.value = _state.value.copy(
                    allEvents = events,
                    filteredEvents = applyFilters(events, _state.value),
                    isLoading = false, isRefreshing = false
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false, isRefreshing = false,
                    error = "Feed unavailable: ${e.message?.take(60)}"
                )
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        loadFeed()
    }

    fun filterByType(type: EventType?) {
        _state.value = _state.value.copy(selectedType = type).let {
            it.copy(filteredEvents = applyFilters(it.allEvents, it))
        }
    }

    fun setSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query).let {
            it.copy(filteredEvents = applyFilters(it.allEvents, it))
        }
    }

    fun filterByRegion(region: String?) {
        _state.value = _state.value.copy(selectedRegion = region).let {
            it.copy(filteredEvents = applyFilters(it.allEvents, it))
        }
    }

    private fun applyFilters(events: List<OsintEvent>, state: FeedUiState): List<OsintEvent> {
        var result = events
        state.selectedType?.let { type -> result = result.filter { it.eventType == type } }
        state.selectedRegion?.let { region -> result = result.filter { it.region == region } }
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.source.lowercase().contains(q) ||
                it.region.lowercase().contains(q)
            }
        }
        return result.sortedByDescending { it.severity * 1000L + it.timestamp / 1000L }
    }
}
