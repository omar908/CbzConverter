package com.puchunguita.cbzconverter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Boolean.FALSE
import java.util.logging.Level
import java.util.logging.Logger

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val NOTHING_PROCESSING = "Nothing Processing"
        private const val STORAGE_PERMISSION_CODE = 1001
    }

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val logger = Logger.getLogger(MainViewModel::class.java.name)

    private val _isCurrentlyConverting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCurrentlyConverting = _isCurrentlyConverting.asStateFlow()

    private val _currentTaskStatus: MutableStateFlow<String> = MutableStateFlow(NOTHING_PROCESSING)
    val currentTaskStatus = _currentTaskStatus.asStateFlow()

    private val _currentSubTaskStatus: MutableStateFlow<String> = MutableStateFlow(NOTHING_PROCESSING)
    val currentSubTaskStatus = _currentSubTaskStatus.asStateFlow()

    private val _maxNumberOfPages: MutableStateFlow<Int> = MutableStateFlow(100)
    val maxNumberOfPages = _maxNumberOfPages.asStateFlow()

    private val _overrideSortOrderToUseOffset: MutableStateFlow<Boolean> = MutableStateFlow(FALSE)
    val overrideSortOrderToUseOffset = _overrideSortOrderToUseOffset.asStateFlow()

    private fun convertCbzFileNameToPdfFileName(fileName: String) : String {
        return fileName.replace(".cbz", ".pdf")
    }

    private fun toggleIsCurrentlyConverting(): Boolean {
        _isCurrentlyConverting.update { currentState -> !currentState }
        return _isCurrentlyConverting.value
    }

    fun toggleOverrideSortOrderToUseOffset(newValue: Boolean) {
        _overrideSortOrderToUseOffset.update { newValue }
    }

    private suspend fun updateConversionState() {
        withContext(Dispatchers.Main) {
            logger.info(if (toggleIsCurrentlyConverting()) "Conversion started" else "Conversion ended")
        }
    }

    private suspend fun updateCurrentTaskStatusMessageSuspend(message: String, level: Level = Level.INFO) {
        withContext(Dispatchers.Main) {
            updateCurrentTaskStatusMessage(message)
            logger.log(level, message)
        }
    }

    private fun updateCurrentTaskStatusMessage(message: String) {
            _currentTaskStatus.update { message }
    }

    private suspend fun updateCurrentSubTaskStatusStatusMessage(message: String) {
        withContext(Dispatchers.Main) {
            _currentSubTaskStatus.update { message }
            logger.info(message)
        }
    }

    private fun updateMaxNumberOfPages(maxNumberOfPages: Int) {
        _maxNumberOfPages.update { maxNumberOfPages }
    }

    fun updateMaxNumberOfPagesSizeFromUserInput(maxNumberOfPages: String) {
        try {
            updateMaxNumberOfPages(maxNumberOfPages.trim().toInt())
            updateCurrentTaskStatusMessage("Updated maxNumberOfPages size: $maxNumberOfPages")
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid maxNumberOfPages size: $maxNumberOfPages reverting to default value")
            updateMaxNumberOfPages(10)
        }
    }

    private suspend fun showToastAndUpdateStatusMessage(
        message: String,
        toastLength: Int,
        loggerLevel: Level = Level.INFO
    ) {
        updateCurrentTaskStatusMessageSuspend(message, loggerLevel)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, toastLength).show()
        }
    }

    fun convertToPDF(fileUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            updateConversionState()
            try {
                val originalCbzFileName = fileUri.getFileName(context)
                val pdfFileName = convertCbzFileNameToPdfFileName(originalCbzFileName)

                updateCurrentTaskStatusMessageSuspend(message = "Conversion from CBZ to PDF started")
                val pdfFiles = convertCbzToPDF(
                    fileUri = fileUri,
                    context = context,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    },
                    maxNumberOfPages = _maxNumberOfPages.value,
                    outputFileName = pdfFileName,
                    overrideSortOrderToUseOffset = _overrideSortOrderToUseOffset.value
                )
                if (pdfFiles.isEmpty()) {
                    throw Exception("No PDF files created, CBZ file is invalid or empty")
                } else {
                    val message = if (pdfFiles.size == 1) "PDF created: ${pdfFiles.first().absolutePath}"
                        else "Multiple PDFs created: ${pdfFiles.joinToString { "\n ${it.absolutePath}" }}"
                    showToastAndUpdateStatusMessage(
                        message = message,
                        toastLength = Toast.LENGTH_LONG
                    )
                }
            } catch (e: Exception) {
                showToastAndUpdateStatusMessage(
                    message = "Conversion failed: ${e.message}",
                    toastLength = Toast.LENGTH_LONG, Level.WARNING
                )
                logger.warning("Conversion failed stacktrace: ${e.stackTrace.contentToString()}")
            } finally {
                updateConversionState()
            }
        }
    }

    fun checkPermissionAndSelectFileAction(
        activity: ComponentActivity,
        filePickerLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                ContextCompat.startActivity(activity, intent, null)
            } else {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        } else {
            val writePermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        }
    }
}
