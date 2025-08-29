package com.theveloper.pixelplay.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val mediaPermissionGranted: Boolean = false,
    val notificationsPermissionGranted: Boolean = false,
    val allFilesAccessGranted: Boolean = false,
) {
    val allPermissionsGranted: Boolean
        get() {
            val mediaOk = mediaPermissionGranted
            val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationsPermissionGranted else true
            val allFilesOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) allFilesAccessGranted else true
            return mediaOk && notificationsOk && allFilesOk
        }
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    fun checkPermissions(context: Context) {
        val mediaPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val notificationsPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13 (Tiramisu)
        }

        val allFilesAccessGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not required before Android 11 (R)
        }

        _uiState.update {
            it.copy(
                mediaPermissionGranted = mediaPermissionGranted,
                notificationsPermissionGranted = notificationsPermissionGranted,
                allFilesAccessGranted = allFilesAccessGranted
            )
        }
    }

    fun setSetupComplete() {
        viewModelScope.launch {
            userPreferencesRepository.setInitialSetupDone(true)
        }
    }
}
