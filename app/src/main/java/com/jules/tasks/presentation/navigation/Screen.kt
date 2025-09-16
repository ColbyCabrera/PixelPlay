package com.jules.tasks.presentation.navigation

sealed class Screen(val route: String) {
    object TaskList : Screen("task_list")
}