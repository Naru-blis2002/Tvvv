package com.example.model

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val channelId: String
)

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannelById(id: String)

    @Query("SELECT channelId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE channelId = :id")
    suspend fun deleteFavoriteById(id: String)
}
