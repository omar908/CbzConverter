package com.puchunguita.cbzconverter

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.getAbsolutePath
import java.io.File
import java.io.InputStream

class ContextHelper(private val context: Context) {
    fun getFileName(uri: Uri): String {
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
        return name
    }

    fun showToast(message: String, length: Int) {
        Toast.makeText(context, message, length).show()
    }

    fun getOutputFolderUri(uri: Uri?): File? {
        val file = uri?.let { DocumentFileCompat.fromUri(context, it) }
        return file?.getAbsolutePath(context)?.let { File(it) }
    }

    fun openInputStream(uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    fun getCacheDir(): File {
        return context.cacheDir
    }

    fun getExternalStoragePublicDirectory(type: String): File {
        return Environment.getExternalStoragePublicDirectory(type)
    }
}
