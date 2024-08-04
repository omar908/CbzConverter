package com.puchunguita.cbzconverter

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.File
import java.io.InputStream
import java.util.logging.Logger
import java.util.stream.IntStream
import java.util.zip.ZipFile
import kotlin.math.ceil

private val logger = Logger.getLogger("com.puchunguita.cbzconverter.ConversionFunction")
fun convertCbzToPDF(
    fileUri: Uri,
    context: Context,
    subStepStatusAction: (String) -> Unit = { status -> logger.info(status) },
    maxNumberOfPages: Int = 100,
    outputFileName: String = "output.pdf"
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
    val totalNumberOfImages = zipFile.size()
    val zipFileEntries = zipFile.entries()

    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!downloadsFolder.exists()) {
        downloadsFolder.mkdirs()
    }
    val outputFiles = mutableListOf<File>()
    var amountOfFilesToExport = 1
    //TODO add error handling to default to the max number of pages to totalNumberOfImages, if maxNumberOfPages is greater than totalNumberOfImages
    var outputFile = File(downloadsFolder, outputFileName)


    if (totalNumberOfImages > maxNumberOfPages) {
        amountOfFilesToExport = ceil(totalNumberOfImages.toDouble() / maxNumberOfPages).toInt()
        val zipFileEntriesList = zipFileEntries.toList()

        IntStream.range(0, amountOfFilesToExport).forEach { index ->
            val newOutputFileName = outputFileName.replace(".pdf", "_part-${index + 1}.pdf")
            outputFile = File(downloadsFolder, newOutputFileName)
            val startIndex = index.times(maxNumberOfPages)
            val nextPossibleEndIndex = index.plus(1).times(maxNumberOfPages)
            val endIndex = if (nextPossibleEndIndex > totalNumberOfImages) totalNumberOfImages else nextPossibleEndIndex
            val imagesToProcess = zipFileEntriesList.subList(startIndex, endIndex)

            PdfWriter(outputFile.absolutePath).use { writer ->
                PdfDocument(writer).use { pdfDoc ->
                    Document(pdfDoc).use { document ->
                        for ((currentImageIndex, imageFile) in imagesToProcess.withIndex()) {
                            subStepStatusAction("Processing part ${index + 1} of $amountOfFilesToExport - Processing image file ${index.times(maxNumberOfPages) + currentImageIndex + 1} of ${imageFiles.size}")
                            val imageInputStream: InputStream
                            try {
                                imageInputStream = zipFile.getInputStream(imageFile)

                                val imageFileByteArray = imageInputStream.readBytes()
                                val imageData = ImageDataFactory.create(imageFileByteArray)
                                val pdfImage = Image(imageData)

                                // Add the image to the PDF document
                                document.add(pdfImage)
                                imageInputStream.close()
                            } catch (e: Exception) {
                                logger.warning("ImageExtraction $e Error processing file ${imageFile.name}")
                            }
                        }
                    }
                }
                outputFiles.add(outputFile)
            }


        }
    } else {
        // Create the PdfWriter and PdfDocument
        PdfWriter(outputFile.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc).use { document ->
                    while (zipFileEntries.hasMoreElements()) {
                        val zipFileEntry = zipFileEntries.nextElement()
                        val imageInputStream: InputStream

                        try {
                            imageInputStream = zipFile.getInputStream(zipFileEntry)

                            val imageFileByteArray = imageInputStream.readBytes()
                            val imageData = ImageDataFactory.create(imageFileByteArray)
                            val pdfImage = Image(imageData)

                            // Add the image to the PDF document
                            document.add(pdfImage)
                            imageInputStream.close()
                        } catch (e: Exception) {
                            logger.warning("ImageExtraction $e Error processing file ${zipFileEntry.name}")
                        }
                    }
                }
            }
            outputFiles.add(outputFile)
        }
    }

    zipFile.close()
    return outputFiles
}