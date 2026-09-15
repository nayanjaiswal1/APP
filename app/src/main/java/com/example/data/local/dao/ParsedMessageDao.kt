package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ParsedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedMessageDao {
    @Query("SELECT * FROM parsed_messages ORDER BY createdAt DESC")
    fun getAllParsedMessages(): Flow<List<ParsedMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParsedMessage(message: ParsedMessageEntity): Long

    @Update
    suspend fun updateParsedMessage(message: ParsedMessageEntity)

    @Delete
    suspend fun deleteParsedMessage(message: ParsedMessageEntity)

    @Query("UPDATE parsed_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
