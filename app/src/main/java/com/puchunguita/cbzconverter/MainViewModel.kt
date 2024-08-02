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

    private suspend fun updateCurrentTaskStatusMessage(message: String, level: Level = Level.INFO) {
        withContext(Dispatchers.Main) {
            _currentTaskStatus.update { message }
            logger.log(level, message)
        }
    }

    private suspend fun updateCurrentSubTaskStatusStatusMessage(message: String) {
        withContext(Dispatchers.Main) {
            _currentSubTaskStatus.update { message }
            logger.info(message)
        }
    }

    private suspend fun showToastAndUpdateStatusMessage(
        message: String,
        toastLength: Int,
        loggerLevel: Level = Level.INFO
    ) {
        updateCurrentTaskStatusMessage(message, loggerLevel)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, toastLength).show()
        }
    }

    fun convertToPDF(fileUri: Uri, batchSize: Int = 10) {
        CoroutineScope(Dispatchers.IO).launch {
            updateConversionState()
            try {
                val originalCbzFileName = fileUri.getFileName(context);
                val pdfFileName = convertCbzFileNameToPdfFileName(originalCbzFileName)

                updateCurrentTaskStatusMessage(message = "CBZ Extraction started")
                updateCurrentSubTaskStatusStatusMessage(message = "Processing first batch of $batchSize")
                val bitmaps = extractImagesFromCBZ(
                    fileUri = fileUri,
                    context = context,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    },
                    batchSize = batchSize
                )

                updateCurrentTaskStatusMessage(message = "PDF Creation started")
                val pdfFile = convertToPDF(
                    imageFiles = bitmaps,
                    context = context,
                    outputFileName = pdfFileName,
                    subStepStatusAction = { message: String ->
                        CoroutineScope(Dispatchers.Main).launch {
                            updateCurrentSubTaskStatusStatusMessage(message)
                        }
                    }
                )

                showToastAndUpdateStatusMessage(
                    message = "PDF created: ${pdfFile.absolutePath}",
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
