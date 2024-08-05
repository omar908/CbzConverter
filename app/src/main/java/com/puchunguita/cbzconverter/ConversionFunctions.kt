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
import java.util.Enumeration
import java.util.logging.Logger
import java.util.stream.IntStream
import java.util.zip.ZipEntry
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
    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return mutableListOf<File>()
    val tempFile = copyCbzToCache(context, inputStream)

    // Open the CBZ file as a zip
    val zipFile = ZipFile(tempFile)
    val zipFileEntries = zipFile.entries()
    val totalNumberOfImages = zipFile.size()

    val outputFiles = mutableListOf<File>()
    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    if (!downloadsFolder.exists()) { downloadsFolder.mkdirs() }

    if (totalNumberOfImages > maxNumberOfPages) {
        createMultiplePdfFromCbz(
            totalNumberOfImages,
            maxNumberOfPages,
            zipFileEntries,
            outputFileName,
            downloadsFolder,
            subStepStatusAction,
            zipFile,
            outputFiles
        )
    } else {
        createSinglePdfFromCbz(
            zipFileEntries,
            outputFileName,
            downloadsFolder,
            subStepStatusAction,
            totalNumberOfImages,
            zipFile,
            outputFiles
        )
    }

    zipFile.close()
    tempFile.delete()
    return outputFiles
}

private fun copyCbzToCache(context: Context, inputStream: InputStream): File {
    val tempFile = File(context.cacheDir, "temp.cbz")
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }
    inputStream.close()
    return tempFile
}

private fun createMultiplePdfFromCbz(
    totalNumberOfImages: Int,
    maxNumberOfPages: Int,
    zipFileEntries: Enumeration<out ZipEntry>,
    outputFileName: String,
    downloadsFolder: File?,
    subStepStatusAction: (String) -> Unit,
    zipFile: ZipFile,
    outputFiles: MutableList<File>
) {
    val amountOfFilesToExport = ceil(totalNumberOfImages.toDouble() / maxNumberOfPages).toInt()
    val zipFileEntriesList = zipFileEntries.toList()

    IntStream.range(0, amountOfFilesToExport).forEach { index ->
        val newOutputFileName = outputFileName.replace(".pdf", "_part-${index + 1}.pdf")
        val outputFile = File(downloadsFolder, newOutputFileName)
        val startIndex = index.times(maxNumberOfPages)
        val nextPossibleEndIndex = index.plus(1).times(maxNumberOfPages)
        val endIndex =
            if (nextPossibleEndIndex > totalNumberOfImages) totalNumberOfImages else nextPossibleEndIndex
        val imagesToProcess = zipFileEntriesList.subList(startIndex, endIndex)

        PdfWriter(outputFile.absolutePath).use { writer ->
            PdfDocument(writer).use { pdfDoc ->
                Document(pdfDoc).use { document ->
                    for ((currentImageIndex, imageFile) in imagesToProcess.withIndex()) {
                        subStepStatusAction(
                            "Processing part ${index + 1} of $amountOfFilesToExport " +
                                    "- Processing image file " +
                                    "${index.times(maxNumberOfPages) + currentImageIndex + 1} " +
                                    "of $totalNumberOfImages"
                        )
                        extractImageAndAddToPDFDocument(zipFile, imageFile, document)
                    }
                }
            }
            outputFiles.add(outputFile)
        }
    }
}

private fun createSinglePdfFromCbz(
    zipFileEntries: Enumeration<out ZipEntry>,
    outputFileName: String,
    downloadsFolder: File?,
    subStepStatusAction: (String) -> Unit,
    totalNumberOfImages: Int,
    zipFile: ZipFile,
    outputFiles: MutableList<File>
) {
    val outputFile = File(downloadsFolder, outputFileName)

    PdfWriter(outputFile.absolutePath).use { writer ->
        PdfDocument(writer).use { pdfDoc ->
            Document(pdfDoc).use { document ->
                // Counter used for logging the progress only
                var counter = 1
                while (zipFileEntries.hasMoreElements()) {
                    subStepStatusAction(
                        "Processing image file " +
                                "$counter " +
                                "of $totalNumberOfImages"
                    )
                    extractImageAndAddToPDFDocument(zipFile, zipFileEntries.nextElement(), document)
                    counter++
                }
            }
        }
        outputFiles.add(outputFile)
    }
}

private fun extractImageAndAddToPDFDocument(
    zipFile: ZipFile,
    zipFileEntry: ZipEntry,
    document: Document
) {
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