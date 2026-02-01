package com.mattev02.expirationdate.date_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.scale
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class DateDetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val inputSize = 640

    // Store the model's expected input type (Float32, Uint8, or Int8)
    private var inputDataType: DataType = DataType.FLOAT32

    data class Detection(
        val box: FloatArray,
        val confidence: Float,
        val classId: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Detection
            if (confidence != other.confidence) return false
            if (classId != other.classId) return false
            if (!box.contentEquals(other.box)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = confidence.hashCode()
            result = 31 * result + classId
            result = 31 * result + box.contentHashCode()
            return result
        }
    }

    fun loadModel() {
        if (interpreter == null) {
            try {
                val modelFile = loadModelFile(context, "date_detection_model_int8.tflite")
                interpreter = Interpreter(modelFile)
            } catch (e: Exception) {
                Log.e("YoloV8OCRHelper", "Error loading model: ${e.message}")
            }
        }
    }

    @Suppress("SameParameterValue")
    @Throws(Exception::class)
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        context.assets.openFd(modelName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel: FileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaledBitmap = bitmap.scale(inputSize, inputSize, false)

        val typeSize = if (inputDataType == DataType.FLOAT32) 4 else 1

        val byteBuffer = ByteBuffer.allocateDirect(typeSize * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixelValue in intValues) {
            val r = Color.red(pixelValue)
            val g = Color.green(pixelValue)
            val b = Color.blue(pixelValue)

            when (inputDataType) {
                DataType.FLOAT32 -> {
                    byteBuffer.putFloat(r / 255.0f)
                    byteBuffer.putFloat(g / 255.0f)
                    byteBuffer.putFloat(b / 255.0f)
                }
                else -> {
                    byteBuffer.put((r - 128).toByte())
                    byteBuffer.put((g - 128).toByte())
                    byteBuffer.put((b - 128).toByte())
                }
            }
        }
        return byteBuffer
    }

    private fun runYoloInference(input: ByteBuffer): Array<Array<FloatArray>>? {
        return try {
            val output = Array(1) { Array(5) { FloatArray(8400) } }
            interpreter?.run(input, output)
            output
        } catch (e: Exception) {
            Log.e("YoloV8OCRHelper", "Inference error: ${e.message}")
            null
        }
    }

    private fun processYoloOutput(output: Array<Array<FloatArray>>, imgWidth: Int, imgHeight: Int): List<Detection> {
        val detections = mutableListOf<Detection>()
        val rawData = output[0]
        val numElements = rawData[0].size

        for (i in 0 until numElements) {
            val confidence = rawData[4][i]

            if (confidence > 0.15f) {
                var xCenter = rawData[0][i]
                var yCenter = rawData[1][i]
                var widthVal = rawData[2][i]
                var heightVal = rawData[3][i]

                if (widthVal <= 1.0f && heightVal <= 1.0f && widthVal > 0f) {
                    xCenter *= inputSize
                    yCenter *= inputSize
                    widthVal *= inputSize
                    heightVal *= inputSize
                }

                val x1 = (xCenter - widthVal / 2f) * (imgWidth.toFloat() / inputSize)
                val y1 = (yCenter - heightVal / 2f) * (imgHeight.toFloat() / inputSize)
                val x2 = (xCenter + widthVal / 2f) * (imgWidth.toFloat() / inputSize)
                val y2 = (yCenter + heightVal / 2f) * (imgHeight.toFloat() / inputSize)

                detections.add(
                    Detection(
                        floatArrayOf(
                            max(0f, x1), max(0f, y1), min(imgWidth.toFloat(), x2), min(imgHeight.toFloat(), y2)
                        ),
                        confidence,
                        0
                    )
                )
            }
        }
        return detections
    }

    private fun performOCR(bitmap: Bitmap, box: FloatArray): String {
        val imgWidth = bitmap.width
        val imgHeight = bitmap.height

        val x1 = box[0].toInt().coerceIn(0, imgWidth - 1)
        val y1 = box[1].toInt().coerceIn(0, imgHeight - 1)
        val x2 = box[2].toInt().coerceIn(0, imgWidth)
        val y2 = box[3].toInt().coerceIn(0, imgHeight)

        var cropWidth = x2 - x1
        var cropHeight = y2 - y1

        if (x1 + cropWidth > imgWidth) cropWidth = imgWidth - x1
        if (y1 + cropHeight > imgHeight) cropHeight = imgHeight - y1

        if (cropWidth <= 0 || cropHeight <= 0) return ""

        return try {
            val croppedBitmap = Bitmap.createBitmap(bitmap, x1, y1, cropWidth, cropHeight)
            val image = InputImage.fromBitmap(croppedBitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val resultText = Tasks.await(recognizer.process(image))
            resultText.text
        } catch (e: Exception) {
            Log.e("YoloV8OCRHelper", "OCR Error: ${e.message}")
            ""
        }
    }

    private fun extractDate(text: String): String? {
        val patterns = listOf(
            Regex("""\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}"""),
            Regex("""\d{1,2}[/.-]\d{4}"""),
            Regex("""\d{4}[/.-]\d{1,2}[/.-]\d{1,2}""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.value
            }
        }
        return null
    }

    private fun convertDateToStandardFormat(dateString: String): String {
        val standardFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val possibleFormats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
            SimpleDateFormat("dd.MM.yy", Locale.getDefault()),
            SimpleDateFormat("MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("MM-yyyy", Locale.getDefault())
        )

        for (format in possibleFormats) {
            try {
                format.isLenient = false
                val date = format.parse(dateString)
                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    if (calendar.get(Calendar.YEAR) < 2000) {
                        calendar.add(Calendar.YEAR, 2000)
                    }
                    return standardFormat.format(calendar.time)
                }
            } catch (_: Exception) {}
        }
        return dateString
    }

    fun detectAndExtractText(bitmap: Bitmap): String? {
        val input = preprocessBitmap(bitmap)
        val output = runYoloInference(input) ?: return null
        val detections = processYoloOutput(output, bitmap.width, bitmap.height)

        if (detections.isNotEmpty()) {
            val bestDetection = detections.maxByOrNull { it.confidence }
            val rawText = bestDetection?.let { performOCR(bitmap, it.box) }

            if (!rawText.isNullOrEmpty()) {
                val extractedDate = extractDate(rawText)
                return if (extractedDate != null) {
                    convertDateToStandardFormat(extractedDate)
                } else {
                    null
                }
            }
        }
        return null
    }

    fun close() {
        interpreter?.close()
    }
}