package com.puchunguita.cbzconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
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
//                val bitmap = decodeSampledBitmapFromStream(imageInputStream, 800, 600) // Adjust size as needed

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

//fun decodeSampledBitmapFromStream(
//    inputStream: InputStream,
//    reqWidth: Int,
//    reqHeight: Int
//): Bitmap? {
//    // First decode with inJustDecodeBounds=true to check dimensions
//    val options = BitmapFactory.Options().apply {
//        inJustDecodeBounds = true
//    }
//
//    // Check if the input stream is valid
//    val bitmapStream = try {
//        BitmapFactory.decodeStream(inputStream, null, options)
//    } catch (e: Exception) {
//        println("BitmapDecoding $e Failed to decode image with inJustDecodeBounds=true")
//        null
//    }
//
//    if (bitmapStream == null) {
//        println("BitmapDecoding Bitmap decoding with inJustDecodeBounds failed")
//        return null
//    }
//
//    // Calculate inSampleSize
//    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
//
//    // Decode bitmap with inSampleSize set
//    options.inJustDecodeBounds = false
//
//    // Reset the input stream for the actual decoding
//    inputStream.reset()
//
//    return try {
//        BitmapFactory.decodeStream(inputStream, null, options)
//    } catch (e: Exception) {
//        println("BitmapDecoding $e Failed to decode image with inSampleSize=${options.inSampleSize}")
//        null
//    }
//}
//
//fun calculateInSampleSize(
//    options: BitmapFactory.Options,
//    reqWidth: Int,
//    reqHeight: Int
//): Int {
//    // Raw height and width of image
//    val (height: Int, width: Int) = options.run {
//        outHeight to outWidth
//    }
//
//    var inSampleSize = 1
//
//    if (height > reqHeight || width > reqWidth) {
//        val halfHeight = height / 2
//        val halfWidth = width / 2
//
//        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
//            inSampleSize *= 2
//        }
//    }
//
//    return inSampleSize
//}

//fun decodeSampledBitmapFromStream(
//    inputStream: InputStream,
//    reqWidth: Int,
//    reqHeight: Int
//): Bitmap? {
//    // First decode with inJustDecodeBounds=true to check dimensions
//    val options = BitmapFactory.Options().apply {
//        inJustDecodeBounds = true
//    }
//    BitmapFactory.decodeStream(inputStream, null, options)
//
//    // Calculate inSampleSize
//    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
//
//    // Decode bitmap with inSampleSize set
//    options.inJustDecodeBounds = false
//    return BitmapFactory.decodeStream(inputStream, null, options)
//}
//
//fun calculateInSampleSize(
//    options: BitmapFactory.Options,
//    reqWidth: Int,
//    reqHeight: Int
//): Int {
//    // Raw height and width of image
//    val (height: Int, width: Int) = options.run {
//        outHeight to outWidth
//    }
//
//    var inSampleSize = 1
//
//    if (height > reqHeight || width > reqWidth) {
//        val halfHeight = height / 2
//        val halfWidth = width / 2
//
//        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
//        // height and width larger than the requested height and width.
//        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
//            inSampleSize *= 2
//        }
//    }
//
//    return inSampleSize
//}

//---

//fun extractImagesFromCBZ(fileUri: Uri, context: Context): List<Bitmap> {
//    val images = mutableListOf<Bitmap>()
//    val inputStream = context.contentResolver.openInputStream(fileUri) ?: return images
//
//    // Copy the CBZ file to a temporary location
//    val tempFile = File(context.cacheDir, "temp.cbz")
//    tempFile.outputStream().use { outputStream ->
//        inputStream.copyTo(outputStream)
//    }
//
//    // Open the CBZ file as a zip
//    val zipFile = ZipFile(tempFile)
//    val fileHeaders = zipFile.entries()
//
//    val batchSize = 10
//    var counter = 0
//
//    while (fileHeaders.hasMoreElements()) {
//        val batch = mutableListOf<Bitmap>()
//
//        // Process files in batches of `batchSize`
//        repeat(batchSize) {
//            if (!fileHeaders.hasMoreElements()) return@repeat // Exit loop if no more files
//
//            val fileHeader = fileHeaders.nextElement()
//            try {
//                val imageInputStream = zipFile.getInputStream(fileHeader)
//                val bitmap = BitmapFactory.decodeStream(imageInputStream)
//                if (bitmap != null) {
//                    batch.add(bitmap)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//        // Add batch of images to the list
//        images.addAll(batch)
//
//        counter += batch.size
//        println("Number of files already processed: $counter - number of files with data: ${images.size} - next item to process ${counter + 1}")
//    }
//
//    // Loop through the files in the CBZ file
////    var counter = 0
////    for (fileHeader in fileHeaders) {
////        try {
////            counter++
////            println("Number of files already processed: $counter - number of files with data: ${images.size} - next item to process ${counter+1}")
////            val imageInputStream = zipFile.getInputStream(fileHeader)
////            val bitmap = BitmapFactory.decodeStream(imageInputStream)
////            if (bitmap != null) {
////                images.add(bitmap)
////            }
////        } catch (e: Exception){
////            e.printStackTrace()
////        }
////
////    }
//
//    tempFile.delete()
//    return images
//}

fun extractImagesFromCBZ(file: File): List<Bitmap> {
    val images = mutableListOf<Bitmap>()
    val zipFile = ZipFile(file)
    val fileHeaders = zipFile.entries()

    for (fileHeader in fileHeaders) {
        val inputStream = zipFile.getInputStream(fileHeader)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        images.add(bitmap)
    }

    return images
}

fun convertToPDF(images: List<Bitmap>, context: Context): File {
    val document = PDDocument()

    for (image in images) {
        val page = PDPage()
        document.addPage(page)

        val contents = PDPageContentStream(document, page)

        // Convert Bitmap to byte array
        val byteArrayOutputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        // Create a PDImageXObject from the byte array
        val pdImage = PDImageXObject.createFromByteArray(document, imageBytes, null)

        // Calculate the position and size of the image on the page
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height

        // Draw the image on the page
        contents.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
        contents.close()
    }

    // Save the PDF to a file
    val outputFile = File(context.filesDir, "output.pdf")
    document.save(outputFile)
    document.close()
    return outputFile


//    val document = PDDocument()
//
//    for (image in images) {
//        val page = PDPage()
//        document.addPage(page)
//
//        val contents = PDPageContentStream(document, page)
//
//        // Create a PDImageXObject from the Bitmap
//        val pdImage = PDImageXObject.createFromByteArray(document, image)
//
//        // Calculate the position and size of the image on the page
//        val pageWidth = page.mediaBox.width
//        val pageHeight = page.mediaBox.height
//
//        // Draw the image on the page
//        contents.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
//        contents.close()
//    }
//
//    // Save the PDF to a file
//    val outputFile = File(context.filesDir, "output.pdf")
//    document.save(outputFile)
//    document.close()
//    return outputFile



//    val document = PDDocument()
//    for (image in images) {
//        val page = PDPage()
//        document.addPage(page)
//
//        val contents = PDPageContentStream(document, page
//        final BufferedImage image = ImageIO.read(new File("graphics/a.jpg"));
//        BufferedImage(image)
//        val pdImage = LosslessFactory.createFromImage(document, image)
//        contents.drawImage(pdImage, 0f, 0f)
//        contents.close()
//    }
//
//    val outputFile = File(context.filesDir, "output.pdf")
//    document.save(outputFile)
//    document.close()
//    return outputFile
}
