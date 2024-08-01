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
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val logger = Logger.getLogger(MainViewModel::class.java.name)

    private val _isCurrentlyConverting: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(false)
    val isCurrentlyConverting = _isCurrentlyConverting.asStateFlow()

    private val _currentTaskStatus: MutableStateFlow<String> = MutableStateFlow<String>("Nothing Processing")
    val currentTaskStatus = _currentTaskStatus.asStateFlow()


    // TODO implement EPUB or remove all traces of it, if this is for manga might be best to remove fully
    fun convertToEPUB(fileUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            // Add your conversion logic here
            withContext(Dispatchers.Main) {
                // Update UI after conversion if needed
            }
        }
    }

    fun convertToPDF(fileUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            updateConversionState()
            try {
                updateStatusMessage(message = "CBZ Extraction started")
                val bitmaps = extractImagesFromCBZ(fileUri, context)

                updateStatusMessage(message = "PDF Creation started")
                val pdfFile = convertToPDF(bitmaps, context)

                showToastAndUpdateStatusMessage(
                    message = "PDF created: ${pdfFile.absolutePath}",
                    toastLength = Toast.LENGTH_LONG
                )
            } catch (e: Exception) {
                showToastAndUpdateStatusMessage(
                    message = "Conversion failed: ${e.message}",
                    toastLength = Toast.LENGTH_LONG, Level.WARNING
                )
                logger.warning("Conversion failed stacktrace: ${e.stackTrace}")
            } finally {
                updateConversionState()
            }
        }
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

    private suspend fun updateStatusMessage(message: String, level: Level = Level.INFO) {
        withContext(Dispatchers.Main) {
            _currentTaskStatus.update { message }
            logger.log(level, message)
        }
    }

    private suspend fun showToastAndUpdateStatusMessage(
        message: String,
        toastLength: Int,
        loggerLevel: Level = Level.INFO
    ) {
        updateStatusMessage(message, loggerLevel)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, toastLength).show()
        }
    }


}
