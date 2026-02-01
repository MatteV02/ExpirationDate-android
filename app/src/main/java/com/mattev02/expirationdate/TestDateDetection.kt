package com.mattev02.expirationdate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.mattev02.expirationdate.date_detection.DateDetector
import java.io.File

/**
 * Activity for DateDetector testing.
 */
class TestDateDetection : AppCompatActivity() {
    private lateinit var dateDetector: DateDetector
    private lateinit var textView: TextView
    private lateinit var photoUri: Uri
    private lateinit var tempFile: File

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            takePicture()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            processCapturedImage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_date_detection)

        dateDetector = DateDetector(this)
        textView = findViewById(R.id.text)
        val btnCamera: Button = findViewById(R.id.take_picture)

        btnCamera.setOnClickListener {
            checkPermissionAndOpenCamera()
        }
    }

    private fun checkPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePicture()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePicture() {
        // Create a temporary file in the cache directory
        tempFile = File.createTempFile("photo_", ".jpg", cacheDir)

        // Get the URI using the FileProvider defined in Manifest
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            tempFile
        )

        // Launch camera
        takePictureLauncher.launch(photoUri)
    }

    private fun processCapturedImage() {
        textView.text = "Processing..."

        Thread {
            try {
                // Decode the file to Bitmap
                var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

                // Fix Rotation (Camera images are often rotated 90 degrees)
                bitmap = rotateBitmapIfRequired(bitmap, tempFile.absolutePath)

                // Run Detection
                val extractedText = dateDetector.detectAndExtractText(bitmap)

                runOnUiThread {
                    if (!extractedText.isNullOrEmpty()) {
                        Log.d("OCR Result", "Text found: $extractedText")
                        textView.text = extractedText
                    } else {
                        textView.text = "No date detected"
                    }
                    // Clean up temp file
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e("TestActivity", "Error: ${e.message}")
                runOnUiThread { textView.text = "Error: ${e.message}" }
            }
        }.start()
    }

    // Helper to fix image rotation from EXIF data
    private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onDestroy() {
        dateDetector.close()
        super.onDestroy()
    }
}