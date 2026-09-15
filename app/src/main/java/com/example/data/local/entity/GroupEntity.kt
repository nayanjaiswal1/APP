package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Trip",
    val iconName: String = "flight",
    val coverColorHex: String = "#00B77D",
    val memberIds: String = "1", // comma separated user IDs e.g. "1,2,3"
    val createdAt: Long = System.currentTimeMillis()
)
