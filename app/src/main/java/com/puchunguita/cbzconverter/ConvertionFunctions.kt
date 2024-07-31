package com.puchunguita.cbzconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.File
import java.util.zip.ZipFile

//TODO implement actual logger for kotlin, and replace all println with logger
fun extractImagesFromCBZ(fileUri: Uri, context: Context): List<File> {
    val imageFiles = mutableListOf<File>()
    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return imageFiles

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
        val batchFiles = mutableListOf<File>()

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
                    // Save bitmap to file
                    val imageFile = File(context.cacheDir, "${System.currentTimeMillis()}.png")
                    imageFile.outputStream().use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    batchFiles.add(imageFile)
                    bitmap.recycle() // Recycle bitmap after saving
                }
                imageInputStream.close()
            } catch (e: Exception) {
                println("ImageExtraction $e Error processing file ${fileHeader.name}")
            }
        }

        // Add batch of image files to the list
        imageFiles.addAll(batchFiles)
        counter += batchFiles.size
        println("Number of files already processed: $counter - number of files with data: ${imageFiles.size} - next item to process ${counter + 1}")
    }

    // Clean up temporary file
    tempFile.delete()

    return imageFiles
}


fun convertToPDF(imageFiles: List<File>, context: Context): File {
    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsFolder.exists()) {
        downloadsFolder.mkdirs()
    }

    // Define the output file for the PDF
    // TODO need to adjust so name matches original name of file being converted
    val outputFile = File(downloadsFolder, "output.pdf")

    // Log the start of the PDF conversion process
    println("Starting PDF conversion...")

    // Create the PdfWriter and PdfDocument
    PdfWriter(outputFile.absolutePath).use { writer ->
        PdfDocument(writer).use { pdfDoc ->
            Document(pdfDoc).use { document ->
                // Loop through each image file and add it to the PDF
                for ((index, imageFile) in imageFiles.withIndex()) {
                    println("Processing image file ${index + 1} of ${imageFiles.size}: ${imageFile.absolutePath}")

                    // Convert the image file to iText image
                    val imageData = ImageDataFactory.create(imageFile.absolutePath)
                    val pdfImage = Image(imageData)

                    // Add the image to the PDF document
                    document.add(pdfImage)
                }
                println("PDF conversion completed.")
            }
        }
    }

    println("PDF saved to ${outputFile.absolutePath}")
    return outputFile
}