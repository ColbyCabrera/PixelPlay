package com.jules.tasks.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority {
    LOW,
    MEDIUM,
    HIGH
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val creationDate: Long,
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM
)
