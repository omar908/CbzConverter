package com.puchunguita.cbzconverter

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

class MainViewModel(private val contextHelper: ContextHelper) : ViewModel() {

    companion object {
        private const val NOTHING_PROCESSING = "Nothing Processing"
        private const val NO_FILE_SELECTED = "No file selected"
        const val EMPTY_STRING = ""
    }

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

    private val _selectedFileName: MutableStateFlow<String> = MutableStateFlow(NO_FILE_SELECTED)
    val selectedFileName = _selectedFileName.asStateFlow()

    private val _selectedFileUri: MutableStateFlow<List<Uri>> = MutableStateFlow(emptyList())
    val selectedFileUri = _selectedFileUri.asStateFlow()

    private val _overrideFileName: MutableStateFlow<String> = MutableStateFlow(EMPTY_STRING)
    val overrideFileName = _overrideFileName.asStateFlow()

    private val _overrideOutputDirectoryUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val overrideOutputDirectoryUri = _overrideOutputDirectoryUri.asStateFlow()

    private fun convertListOfCbzFileNameToPdfFileName(fileNames: List<String>) : List<String> {
        return fileNames.stream().map{ it.replace(".cbz", ".pdf") }.collect(Collectors.toList())
    }

    private fun toggleIsCurrentlyConverting(forceUpdate: Boolean): Boolean {
        _isCurrentlyConverting.update { forceUpdate }
        return _isCurrentlyConverting.value
    }

    fun toggleOverrideSortOrderToUseOffset(newValue: Boolean) {
        _overrideSortOrderToUseOffset.update { newValue }
    }

    private suspend fun updateConversionState(forceUpdate: Boolean) {
        withContext(Dispatchers.Main) {
            logger.info(
                if (toggleIsCurrentlyConverting(forceUpdate)) "Conversion started" else "Conversion ended"
            )
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

    private fun updateOverrideFileName(newOverrideFileName: String) {
        _overrideFileName.update { newOverrideFileName }
    }

    fun updateOverrideFileNameFromUserInput(newOverrideFileName: String) {
        try {
            if (newOverrideFileName.isBlank()) throw Exception("Blank overrideFileName")
            updateOverrideFileName(newOverrideFileName)
            updateCurrentTaskStatusMessage("Updated overrideFileName: $newOverrideFileName")
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid overrideFileName: $newOverrideFileName reverting to empty value")
            updateOverrideFileName(EMPTY_STRING)
        }
    }

    private fun updateSelectedFileName(newSelectedFileName: String) {
        _selectedFileName.update { newSelectedFileName }
    }

    private fun updateSelectedFileNameFromUserInput(newSelectedFileNames: String) {
        try {
            if (newSelectedFileNames.isBlank()) throw Exception("Blank fileName")
            updateSelectedFileName(newSelectedFileNames)
            updateOverrideFileNameFromUserInput(EMPTY_STRING)
            updateCurrentTaskStatusMessage("Updated selectedFileName: $newSelectedFileNames")
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid selectedFileName: $newSelectedFileNames reverting to empty value")
            updateSelectedFileName(EMPTY_STRING)
        }
    }

    private fun updateSelectedFileUri(newSelectedFileUri: List<Uri>) {
        _selectedFileUri.update { newSelectedFileUri }
    }

    fun updateUpdateSelectedFileUriFromUserInput(newSelectedFileUris: List<Uri>) {
        try {
            updateSelectedFileUri(newSelectedFileUris)
            val separator = "\n"
            val selectedFileNames = newSelectedFileUris.stream().map { selectedFileUri -> selectedFileUri.getFileName() }.collect(
                Collectors.toList()).joinToString(separator)
            updateSelectedFileNameFromUserInput(selectedFileNames)
            updateCurrentTaskStatusMessage("Updated SelectedFileUri: $newSelectedFileUris")
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid SelectedFileUri: $newSelectedFileUris reverting to empty value")
            updateSelectedFileUri(emptyList())
        }
    }

    private fun updateOverrideOutputDirectoryUri(newOverrideOutputPath: Uri?) {
        _overrideOutputDirectoryUri.update { newOverrideOutputPath }
    }

    fun updateOverrideOutputPathFromUserInput(newOverrideOutputPath: Uri) {
        try {
            updateOverrideOutputDirectoryUri(newOverrideOutputPath)
            updateCurrentTaskStatusMessage("Updated overrideOutputPath: $newOverrideOutputPath")
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid overrideOutputPath: $newOverrideOutputPath reverting to empty value")
            updateOverrideOutputDirectoryUri(null)
        }
    }

    private suspend fun showToastAndUpdateStatusMessage(
        message: String,
        toastLength: Int,
        loggerLevel: Level = Level.INFO
    ) {
        updateCurrentTaskStatusMessageSuspend(message, loggerLevel)
        withContext(Dispatchers.Main) {
            contextHelper.showToast(message, toastLength)
        }
    }

    fun convertToPDF(fileUris: List<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            updateConversionState(TRUE)
            try {
                val pdfFileName = getFileNameForPdf(fileUris)
                val outputFolder = getOutputFolder()

                updateCurrentTaskStatusMessageSuspend(message = "Conversion from CBZ to PDF started")
                val pdfFiles = convertCbzToPDF(
                    fileUri = fileUris,
                    contextHelper = contextHelper,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    },
                    maxNumberOfPages = _maxNumberOfPages.value,
                    outputFileNames = pdfFileName,
                    overrideSortOrderToUseOffset = _overrideSortOrderToUseOffset.value,
                    outputDirectory = outputFolder
                )
                checkPdfsFilesSizeAndUpdateStatus(pdfFiles)
            } catch (e: Exception) {
                showToastAndUpdateStatusMessage(
                    message = "Conversion failed: ${e.message}",
                    toastLength = Toast.LENGTH_LONG, Level.WARNING
                )
                logger.warning("Conversion failed stacktrace: ${e.stackTrace.contentToString()}")
            } finally {
                updateConversionState(FALSE)
            }
        }
    }

    private suspend fun checkPdfsFilesSizeAndUpdateStatus(pdfFiles: List<File>) {
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
    }

    private fun getFileNameForPdf(filesUri: List<Uri>): List<String> {
        var fileUri = filesUri.stream().map {it.getFileName()}.collect(Collectors.toList())

        if (_overrideFileName.value != EMPTY_STRING) {
            fileUri = if(fileUri.size == 1) {
                List(1) { _overrideFileName.value.plus(".cbz") }
            } else {
                List(fileUri.size) { index ->
                    _overrideFileName.value + "_${index + 1}.cbz"
                }
            }
        }
        return convertListOfCbzFileNameToPdfFileName(fileUri)
    }

    private fun getOutputFolder(): File {
        var outputFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (_overrideOutputDirectoryUri.value != null) {
            outputFolder = contextHelper.getOutputFolderUri(_overrideOutputDirectoryUri.value)
        }
        return outputFolder
    }

    fun checkPermissionAndSelectFileAction(
        activity: ComponentActivity,
        filePickerLauncher: ManagedActivityResultLauncher<Array<String>, List<Uri>>
    ) {
        PermissionsManager.checkPermissionAndSelectFileAction(activity, filePickerLauncher)
    }

    fun checkPermissionAndSelectDirectoryAction(
        activity: ComponentActivity,
        directoryPickerLauncher: ManagedActivityResultLauncher<Uri?, Uri?>
    ) {
        PermissionsManager.checkPermissionAndSelectDirectoryAction(activity, directoryPickerLauncher)
    }

   private fun Uri.getFileName(): String {
        return contextHelper.getFileName(this)
    }
}
