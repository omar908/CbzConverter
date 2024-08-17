package com.puchunguita.cbzconverter

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
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.ceil
import kotlin.streams.asStream

private val logger = Logger.getLogger("com.puchunguita.cbzconverter.ConversionFunction")
fun convertCbzToPDF(
    fileUri: List<Uri>,
    contextHelper: ContextHelper,
    subStepStatusAction: (String) -> Unit = { status -> logger.info(status) },
    maxNumberOfPages: Int = 100,
    outputFileNames: List<String> = List(fileUri.size) { index -> "output_$index.pdf" },
    overrideSortOrderToUseOffset: Boolean = false,
    outputDirectory: File = contextHelper.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
): List<File> {
    if (fileUri.isEmpty()) { return mutableListOf() }
    val outputFiles = mutableListOf<File>()

    fileUri.forEachIndexed  { index, uri ->
        val outputFileName = outputFileNames[index]

        val inputStream = contextHelper.openInputStream(uri) ?: run {
            subStepStatusAction("Could not copy CBZ file to cache: $outputFileName"); return@forEachIndexed
        }

        val tempFile = copyCbzToCacheAndCloseInputStream(contextHelper, inputStream)

        // Open the CBZ file as a zip
        val zipFile = ZipFile(tempFile)

        val totalNumberOfImages = zipFile.size()
        if (totalNumberOfImages == 0) { subStepStatusAction("No images found in CBZ file: $outputFileName"); return@forEachIndexed }

        val zipFileEntriesList = orderZipEntriesToList(overrideSortOrderToUseOffset, zipFile)

        if (!outputDirectory.exists()) { outputDirectory.mkdirs() }

        if (totalNumberOfImages > maxNumberOfPages) {
            createMultiplePdfFromCbz(
                totalNumberOfImages,
                maxNumberOfPages,
                zipFileEntriesList,
                outputFileName,
                outputDirectory,
                subStepStatusAction,
                zipFile,
                outputFiles
            )
        } else {
            createSinglePdfFromCbz(
                zipFileEntriesList,
                outputFileName,
                outputDirectory,
                subStepStatusAction,
                totalNumberOfImages,
                zipFile,
                outputFiles
            )
        }

        zipFile.close()
        tempFile.delete()

    }

    return outputFiles
}

private fun orderZipEntriesToList(
    overrideSortOrderToUseOffset: Boolean,
    zipFile: ZipFile
): List<ZipEntry> {
    val zipFileEntriesStream = zipFile.entries().asSequence().asStream()

    // Without `.sorted` it goes based upon order in zip which uses a field called offset,
        // this order is inherited through zipFile.stream().
    // Using `.sorted`, sorts by file name in ascending order
    return if (overrideSortOrderToUseOffset) {
        zipFileEntriesStream.collect(Collectors.toList())
    } else {
        zipFileEntriesStream
            .sorted { f1, f2 -> f1.name.compareTo(f2.name) }
            .collect(Collectors.toList())
    }
}

private fun copyCbzToCacheAndCloseInputStream(contextHelper: ContextHelper, inputStream: InputStream): File {
    val tempFile = File(contextHelper.getCacheDir(), "temp.cbz")
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }
    inputStream.close()
    return tempFile
}

private fun createMultiplePdfFromCbz(
    totalNumberOfImages: Int,
    maxNumberOfPages: Int,
    zipFileEntriesList: List<ZipEntry>,
    outputFileName: String,
    outputDirectory: File?,
    subStepStatusAction: (String) -> Unit,
    zipFile: ZipFile,
    outputFiles: MutableList<File>
) {
    val amountOfFilesToExport = ceil(totalNumberOfImages.toDouble() / maxNumberOfPages).toInt()

    IntStream.range(0, amountOfFilesToExport).forEach { index ->
        val newOutputFileName = outputFileName.replace(".pdf", "_part-${index + 1}.pdf")
        val outputFile = File(outputDirectory, newOutputFileName)
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
    zipFileEntriesList: List<ZipEntry>,
    outputFileName: String,
    outputDirectory: File?,
    subStepStatusAction: (String) -> Unit,
    totalNumberOfImages: Int,
    zipFile: ZipFile,
    outputFiles: MutableList<File>
) {
    val outputFile = File(outputDirectory, outputFileName)

    PdfWriter(outputFile.absolutePath).use { writer ->
        PdfDocument(writer).use { pdfDoc ->
            Document(pdfDoc).use { document ->
                for ((currentImageIndex, imageFile) in zipFileEntriesList.withIndex()) {
                    subStepStatusAction(
                        "Processing image file " +
                                "${currentImageIndex + 1} " +
                                "of $totalNumberOfImages"
                    )
                    extractImageAndAddToPDFDocument(zipFile, imageFile, document)
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