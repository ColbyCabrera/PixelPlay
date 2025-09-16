package com.jules.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jules.tasks.data.database.Priority
import com.jules.tasks.data.database.Task
import com.jules.tasks.data.database.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskDao: TaskDao
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            if (taskDao.getAllTasks().first().isEmpty()) {
                addPreMadeTasks()
            }
        }
    }

    private fun addPreMadeTasks() {
        viewModelScope.launch {
            taskDao.insertTask(
                Task(
                    title = "Schedule a dentist appointment",
                    description = "Check for available slots next week.",
                    isCompleted = false,
                    creationDate = System.currentTimeMillis(),
                    priority = Priority.HIGH
                )
            )
            taskDao.insertTask(
                Task(
                    title = "Pay monthly bills",
                    description = "Electricity, water, and internet.",
                    isCompleted = false,
                    creationDate = System.currentTimeMillis(),
                    priority = Priority.MEDIUM
                )
            )
            taskDao.insertTask(
                Task(
                    title = "Go grocery shopping",
                    description = "Milk, eggs, bread, and cheese.",
                    isCompleted = false,
                    creationDate = System.currentTimeMillis(),
                    priority = Priority.LOW
                )
            )
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }
}
