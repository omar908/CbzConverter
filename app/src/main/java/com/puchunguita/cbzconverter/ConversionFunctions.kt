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
import java.util.logging.Logger
import java.util.stream.IntStream
import java.util.zip.ZipFile
import kotlin.math.ceil

private val logger = Logger.getLogger("com.puchunguita.cbzconverter.ConversionFunction")
fun extractImagesFromCBZ(
    fileUri: Uri,
    context: Context,
    subStepStatusAction: (String) -> Unit = { status -> logger.info(status) },
    batchSize: Int = 10
): List<File> {
    val imageFiles = mutableListOf<File>()
    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return imageFiles

    // Copy the CBZ file to a temporary location
    val tempFile = File(context.cacheDir, "temp.cbz")
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }

    // Open the CBZ file as a zip
    val zipFile = ZipFile(tempFile)
    val zipFileEntries = zipFile.entries()

    var counter = 0

    while (zipFileEntries.hasMoreElements()) {
        val batchFiles = mutableListOf<File>()

        // Process files in batches of `batchSize`
        repeat(batchSize) {
            if (!zipFileEntries.hasMoreElements()) return@repeat // Exit loop if no more files

            val zipFileEntry = zipFileEntries.nextElement()
            try {
                val imageInputStream = zipFile.getInputStream(zipFileEntry)
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
                logger.warning("ImageExtraction $e Error processing file ${zipFileEntry.name}")
            }
        }

        // Add batch of image files to the list
        imageFiles.addAll(batchFiles)
        counter += batchFiles.size
        subStepStatusAction("Number of files already processed: $counter - Number of files with actual data: ${imageFiles.size} - Next batch amount to process $batchSize")
    }

    // Clean up temporary file
    tempFile.delete()
    subStepStatusAction("Deleted temporary files")

    return imageFiles
}


fun convertToPDF(
    imageFiles: List<File>,
    context: Context,
    subStepStatusAction: (String) -> Unit = { status -> logger.info(status) },
    outputFileName: String = "output.pdf",
    maxNumberOfPages: Int = 100
): List<File> {
    // Log the start of the PDF conversion process
    subStepStatusAction("Starting PDF conversion...")

    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsFolder.exists()) {
        downloadsFolder.mkdirs()
    }
    val outputFiles = mutableListOf<File>()
    var amountOfFilesToExport = 1
    //TODO add error handling to default to the max number of pages to totalNumberOfImages, if maxNumberOfPages is greater than totalNumberOfImages
    val totalNumberOfImages: Int = imageFiles.size
    // Define the output file for the PDF
    var outputFile = File(downloadsFolder, outputFileName)


    if (totalNumberOfImages > maxNumberOfPages) {
        amountOfFilesToExport = ceil(totalNumberOfImages.toDouble() / maxNumberOfPages).toInt()

        IntStream.range(0, amountOfFilesToExport).forEach { index ->
            val newOutputFileName = outputFileName.replace(".pdf", "_part-${index+1}.pdf")
            outputFile = File(downloadsFolder, newOutputFileName)
            val startIndex = index.times(maxNumberOfPages)
            val nextPossibleEndIndex = index.plus(1).times(maxNumberOfPages)
            val endIndex = if (nextPossibleEndIndex > totalNumberOfImages) totalNumberOfImages else nextPossibleEndIndex
            val imagesToProcess = imageFiles.subList(startIndex, endIndex)

            //TODO Extract duplicate code for creating PDF file

            // Create the PdfWriter and PdfDocument
            PdfWriter(outputFile.absolutePath).use { writer ->
                PdfDocument(writer).use { pdfDoc ->
                    Document(pdfDoc).use { document ->
                        // Loop through each image file and add it to the PDF
                        for ((currentImageIndex, imageFile) in imagesToProcess.withIndex()) {
                            subStepStatusAction("Processing part ${index + 1} of $amountOfFilesToExport - Processing image file ${index.times(maxNumberOfPages) + currentImageIndex + 1} of ${imageFiles.size}: ${imageFile.absolutePath}")

                            // Convert the image file to iText image
                            val imageData = ImageDataFactory.create(imageFile.absolutePath)
                            val pdfImage = Image(imageData)

                            // Add the image to the PDF document
                            document.add(pdfImage)
                        }
                        subStepStatusAction("PDF conversion completed.")
                    }
                }
            }
            outputFiles.add(outputFile)
        }
    } else {
        // Create the PdfWriter and PdfDocument
        PdfWriter(outputFile.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc).use { document ->
                    // Loop through each image file and add it to the PDF
                    for ((index, imageFile) in imageFiles.withIndex()) {
                        subStepStatusAction("Processing image file ${index + 1} of ${imageFiles.size}: ${imageFile.absolutePath}")

                        // Convert the image file to iText image
                        val imageData = ImageDataFactory.create(imageFile.absolutePath)
                        val pdfImage = Image(imageData)

                        // Add the image to the PDF document
                        document.add(pdfImage)
                    }
                    subStepStatusAction("PDF conversion completed.")
                }
            }
        }
        outputFiles.add(outputFile)
    }

    subStepStatusAction("PDF saved to ${outputFiles.first().absolutePath}")
    return outputFiles
}