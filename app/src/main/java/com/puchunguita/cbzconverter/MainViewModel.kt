package com.puchunguita.cbzconverter

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

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
            // Add your conversion logic here
            withContext(Dispatchers.Main) {
                // Update UI after conversion if needed
                val bitmaps = extractImagesFromCBZ(fileUri, context)
                val pdfFile = convertToPDF(bitmaps, context)
                // Handle the PDF file as needed (e.g., show a notification, open it, etc.)

            }
        }
    }
}
