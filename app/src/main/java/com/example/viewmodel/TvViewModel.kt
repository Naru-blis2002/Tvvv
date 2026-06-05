package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppDatabase
import com.example.model.Channel
import com.example.model.ChannelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChannelRepository

    val activeChannel = MutableStateFlow<Channel?>(null)
    val selectedCountry = MutableStateFlow("Todos")
    val searchQuery = MutableStateFlow("")

    val favoriteIds: StateFlow<Set<String>>

    val filteredChannels: StateFlow<List<Channel>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ChannelRepository(database.channelDao())

        favoriteIds = repository.allFavoriteIds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

        // Combine flows to produce the reactive filtered channels list
        filteredChannels = combine(
            repository.allChannels,
            selectedCountry,
            searchQuery,
            favoriteIds
        ) { channels, country, query, favorites ->
            var result = channels

            // 1. Filter by country
            if (country != "Todos") {
                if (country == "Favoritos") {
                    result = result.filter { favorites.contains(it.id) }
                } else {
                    result = result.filter { it.country.equals(country, ignoreCase = true) }
                }
            }

            // 2. Filter by search query
            if (query.isNotBlank()) {
                result = result.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
                }
            }

            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Set default active channel once channels are loaded
        viewModelScope.launch {
            filteredChannels.collect { channels ->
                if (activeChannel.value == null && channels.isNotEmpty()) {
                    activeChannel.value = channels.first()
                }
            }
        }
    }

    fun selectCountry(country: String) {
        selectedCountry.value = country
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    fun selectChannel(channel: Channel) {
        activeChannel.value = channel
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun addCustomChannel(name: String, country: String, url: String, category: String, desc: String) {
        viewModelScope.launch {
            val uniqueId = "custom_${UUID.randomUUID()}"
            val newChannel = Channel(
                id = uniqueId,
                name = name,
                streamUrl = url,
                country = country,
                category = category,
                description = desc,
                logoUrl = "https://images.unsplash.com/photo-1542204172-e7052809a1a4?w=120",
                isCustom = true
            )
            repository.insertChannel(newChannel)
            // Auto play the newly added channel
            activeChannel.value = newChannel
        }
    }
}
