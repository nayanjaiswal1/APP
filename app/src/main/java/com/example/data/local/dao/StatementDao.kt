package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StatementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatementDao {
    @Query("SELECT * FROM statements ORDER BY uploadedAt DESC")
    fun getAllStatements(): Flow<List<StatementEntity>>

    @Query("SELECT * FROM statements WHERE accountId = :accountId ORDER BY uploadedAt DESC")
    fun getStatementsByAccount(accountId: Long): Flow<List<StatementEntity>>

    @Query("SELECT * FROM statements WHERE id = :id")
    suspend fun getStatementById(id: Long): StatementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatement(statement: StatementEntity): Long

    @Update
    suspend fun updateStatement(statement: StatementEntity)

    @Delete
    suspend fun deleteStatement(statement: StatementEntity)

    @Query("DELETE FROM statements WHERE id = :id")
    suspend fun deleteStatementById(id: Long)
}
