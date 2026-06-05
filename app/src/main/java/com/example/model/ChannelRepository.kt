package com.example.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ChannelRepository(private val channelDao: ChannelDao) {

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()

    val allFavoriteIds: Flow<Set<String>> = channelDao.getAllFavoriteIds()
        .map { list -> list.toSet() }

    suspend fun insertChannel(channel: Channel) {
        channelDao.insertChannel(channel)
    }

    suspend fun deleteChannelById(id: String) {
        channelDao.deleteChannelById(id)
    }

    suspend fun toggleFavorite(channelId: String) {
        val currentFavorites = allFavoriteIds.first()
        if (currentFavorites.contains(channelId)) {
            channelDao.deleteFavoriteById(channelId)
        } else {
            channelDao.insertFavorite(Favorite(channelId))
        }
    }
}
