package com.puchunguita.cbzconverter

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Level
import java.util.logging.Logger

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val NOTHING_PROCESSING = "Nothing Processing"
    }

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val logger = Logger.getLogger(MainViewModel::class.java.name)

    private val _isCurrentlyConverting: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(false)
    val isCurrentlyConverting = _isCurrentlyConverting.asStateFlow()

    private val _currentTaskStatus: MutableStateFlow<String> = MutableStateFlow<String>(Companion.NOTHING_PROCESSING)
    val currentTaskStatus = _currentTaskStatus.asStateFlow()

    private val _currentSubTaskStatus: MutableStateFlow<String> = MutableStateFlow<String>(Companion.NOTHING_PROCESSING)
    val currentSubTaskStatus = _currentSubTaskStatus.asStateFlow()

    private val _batchSize: MutableStateFlow<Int> = MutableStateFlow<Int>(10)
    val batchSize = _batchSize.asStateFlow()

    private val _maxNumberOfPages: MutableStateFlow<Int> = MutableStateFlow<Int>(100)
    val maxNumberOfPages = _maxNumberOfPages.asStateFlow()

    private fun convertCbzFileNameToPdfFileName(fileName: String) : String {
        return fileName.replace(".cbz", ".pdf")
    }

    private fun toggleIsCurrentlyConverting(): Boolean {
        _isCurrentlyConverting.update { currentState -> !currentState }
        return _isCurrentlyConverting.value
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

    private fun updateMaxNumberOfPages(batchSize: Int) {
        _maxNumberOfPages.update { batchSize }
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

    private fun updateBatchSize(batchSize: Int) {
        _batchSize.update { batchSize }
    }

    fun updateBatchSizeFromUserInput(batchSize: String) {
        try {
            updateBatchSize(batchSize.toInt())
        } catch (e: Exception) {
            updateCurrentTaskStatusMessage("Invalid batch size: $batchSize reverting to default value")
            updateBatchSize(10)
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
                val originalCbzFileName = fileUri.getFileName(context);
                val pdfFileName = convertCbzFileNameToPdfFileName(originalCbzFileName)

                updateCurrentTaskStatusMessageSuspend(message = "CBZ Extraction started")
                updateCurrentSubTaskStatusStatusMessage(message = "Processing first batch of ${batchSize.value}")
                val bitmaps = extractImagesFromCBZ(
                    fileUri = fileUri,
                    context = context,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    },
                    batchSize = _batchSize.value
                )

                updateCurrentTaskStatusMessageSuspend(message = "PDF Creation started")
                val pdfFiles = convertToPDF(
                    imageFiles = bitmaps,
                    context = context,
                    outputFileName = pdfFileName,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    },
                    maxNumberOfPages = _maxNumberOfPages.value
                )

                //TODO update to make more sense if multiple PDFs are created
                showToastAndUpdateStatusMessage(
                    message = "PDF created: ${pdfFiles.first().absolutePath}",
                    toastLength = Toast.LENGTH_LONG
                )
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




}
