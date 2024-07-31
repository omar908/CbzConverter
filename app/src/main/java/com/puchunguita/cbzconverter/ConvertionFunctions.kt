package com.puchunguita.cbzconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

fun extractImagesFromCBZ(fileUri: Uri, context: Context): List<Bitmap> {
    val images = mutableListOf<Bitmap>()
    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return images

    // Copy the CBZ file to a temporary location
    val tempFile = File(context.cacheDir, "temp.cbz")
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }

    // Open the CBZ file as a zip
    val zipFile = ZipFile(tempFile)
    val fileHeaders = zipFile.entries()

    val batchSize = 10
    var counter = 0

    while (fileHeaders.hasMoreElements()) {
        val batch = mutableListOf<Bitmap>()

        // Process files in batches of `batchSize`
        repeat(batchSize) {
            if (!fileHeaders.hasMoreElements()) return@repeat // Exit loop if no more files

            val fileHeader = fileHeaders.nextElement()
            try {
                val imageInputStream = zipFile.getInputStream(fileHeader)
                // Use BitmapFactory.Options to reduce memory usage if needed
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                }
                val bitmap = BitmapFactory.decodeStream(imageInputStream, null, options)

                if (bitmap != null) {
                    batch.add(bitmap)
                }
                imageInputStream.close()
            } catch (e: Exception) {
                println("ImageExtraction $e Error processing file ${fileHeader.name}")
            }
        }

        // Add batch of images to the list
        images.addAll(batch)
        batch.forEach { it.recycle() } // Recycle bitmaps after processing

        counter += batch.size
        println("Number of files already processed: $counter - number of files with data: ${images.size} - next item to process ${counter + 1}")
    }

    // Clean up temporary file
    tempFile.delete()

    return images
}

fun convertToPDF(images: List<Bitmap>, context: Context): File {
    val outputFile = File(context.filesDir, "output.pdf")

    PdfWriter(outputFile.absolutePath).use { writer ->
        PdfDocument(writer).use { pdfDoc ->
            Document(pdfDoc).use { document ->
                for (image in images) {
                    // Convert Bitmap to byte array
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                    val imageBytes = byteArrayOutputStream.toByteArray()

                    // Convert byte array to iText image
                    val imageData = ImageDataFactory.create(imageBytes)
                    val pdfImage = Image(imageData)

                    // Add image to document
                    document.add(pdfImage)
                }
            }
        }
    }
    return outputFile
}