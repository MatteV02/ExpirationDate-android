package com.mattev02.expirationdate

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.widget.doOnTextChanged
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mattev02.expirationdate.date_detection.DateDetector
import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.itemlist.ItemList
import com.mattev02.expirationdate.itemlist.database.Database
import com.mattev02.expirationdate.itemlist.database.ItemDao
import com.mattev02.expirationdate.itemlist.database.ItemListDao
import com.mattev02.expirationdate.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ItemDetailActivity : AppCompatActivity() {

    var item = Item()
    var itemList = ItemList()

    private lateinit var dateDetector: DateDetector
    private lateinit var photoUri: Uri
    private lateinit var tempFile: File
    private lateinit var expirationDate: TextInputLayout
    private lateinit var itemName : TextInputEditText
    private lateinit var quantity: TextInputEditText
    private lateinit var expirableCheckbox : CheckBox
    private lateinit var takePictureButton : ImageButton
    private lateinit var progressBar : ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_item_detail_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Channel via Helper
        NotificationHelper.createNotificationChannel(this)

        dateDetector = DateDetector(this)

        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.remove_item -> {
                    // Delegate cancellation to Helper
                    NotificationHelper.cancelNotification(this, item)
                    itemList.removeItem(item)
                    finish()
                }
            }
            true
        }

        itemName = findViewById(R.id.item_name)
        itemName.doOnTextChanged { text, _, _, _ ->
            item.name = text.toString()
            // Update notification if name changes
            if (item.taken) NotificationHelper.scheduleNotification(this, item)
        }

        quantity = findViewById(R.id.quantity)
        quantity.doOnTextChanged { text, _, _, _ ->
            try {
                item.quantity = text.toString().toInt()
            } catch (_ : NumberFormatException) {}
        }

        findViewById<ImageButton>(R.id.decrease_quantity_button).setOnClickListener {
            val newQuantity = quantity.text.toString().toInt() - 1
            if (newQuantity > 0) quantity.setText(newQuantity.toString())
        }

        findViewById<ImageButton>(R.id.increase_quantity_button).setOnClickListener {
            val newQuantity = quantity.text.toString().toInt() + 1
            quantity.setText(newQuantity.toString())
        }

        expirableCheckbox = findViewById(R.id.expirable)
        expirableCheckbox.setOnCheckedChangeListener { _, isChecked ->
            item.isExpirable = isChecked
            if (isChecked) {
                checkNotificationPermissionAndSchedule()
                expirationDate.visibility = View.VISIBLE
                takePictureButton.visibility = View.VISIBLE
            } else {
                expirationDate.visibility = View.GONE
                takePictureButton.visibility = View.GONE
                NotificationHelper.cancelNotification(this, item)
            }
        }

        expirationDate = findViewById(R.id.expiration_date)
        expirationDate.setEndIconOnClickListener {
            showDatePicker()
        }

        expirationDate.editText?.doOnTextChanged { text, _, _, _ ->
            try {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                item.expirationDate = LocalDate.parse(text.toString(), formatter)

                // Logic: Update notification immediately via Helper
                if (item.taken) {
                    NotificationHelper.scheduleNotification(this, item)
                }
            } catch (_: Exception) { }
        }

        takePictureButton = findViewById(R.id.take_picture)
        takePictureButton.setOnClickListener {
            checkPermissionAndOpenCamera()
        }

        progressBar = findViewById(R.id.progress_bar)
        progressBar.visibility = View.GONE

        val okButton : FloatingActionButton = findViewById(R.id.ok_button)
        okButton.setOnClickListener { view -> finish() }

        lifecycleScope.launch(Dispatchers.IO) {
            Database.init(this@ItemDetailActivity)
        }
    }

    override fun onResume() {
        super.onResume()

        val itemId = intent.extras?.getInt("itemId")
        val itemListId = intent.extras?.getInt("itemListId")

        lifecycleScope.launch(Dispatchers.IO) {
            if (itemId != null) item = ItemDao.readItemById(itemId.toLong()) ?: item
            if (itemListId != null) itemList = ItemListDao.read(itemListId.toLong()) ?: itemList
            lifecycleScope.launch(Dispatchers.Main) { updateItem(item) }
        }
    }

    private fun updateItem(item: Item) {
        if (!item.taken) {
            findViewById<LinearLayout>(R.id.expiration_date_layout).visibility = View.GONE
        } else {
            expirableCheckbox.isChecked = item.isExpirable
            if (item.isExpirable) {
                expirationDate.visibility = View.VISIBLE
                if (progressBar.isGone) {
                    takePictureButton.visibility = View.VISIBLE
                }
            } else {
                expirationDate.visibility = View.GONE
                takePictureButton.visibility = View.GONE
            }
            expirationDate.editText?.setText(item.expirationDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        }
        itemName.setText(item.name)
        quantity.setText(item.quantity.toString())
    }

    override fun onDestroy() {
        dateDetector.close()
        super.onDestroy()
    }

    // Permission launcher for Android 13+ Notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationHelper.scheduleNotification(this, item)
        }
    }

    private fun checkNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationHelper.scheduleNotification(this, item)
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationHelper.scheduleNotification(this, item)
        }
    }

    private fun showDatePicker() {
        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
        datePickerBuilder.setTitleText(R.string.pick_date)

        if (expirationDate.editText.toString().isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val parsedDate = dateFormat.parse(expirationDate.editText?.text.toString())
                parsedDate?.let { datePickerBuilder.setSelection(it.time) }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val datePicker = datePickerBuilder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            expirationDate.editText?.setText(dateFormat.format(Date(selection)))
        }
        datePicker.addOnNegativeButtonClickListener { expirationDate.clearFocus() }
        datePicker.addOnDismissListener { expirationDate.clearFocus() }
        datePicker.show(supportFragmentManager, "DATE_PICKER_TAG")
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) takePicture() else Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) processCapturedImage()
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePicture() {
        tempFile = File.createTempFile("photo_", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", tempFile)
        lifecycleScope.launch(Dispatchers.IO) {
            dateDetector.loadModel()
        }
        takePictureLauncher.launch(photoUri)
    }

    private fun processCapturedImage() {
        progressBar.visibility = View.VISIBLE
        takePictureButton.visibility = View.GONE
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                bitmap = rotateBitmapIfRequired(bitmap, tempFile.absolutePath)
                val extractedText = dateDetector.detectAndExtractText(bitmap)
                lifecycleScope.launch(Dispatchers.Main) {
                    if (!extractedText.isNullOrEmpty()) {
                        expirationDate.editText?.setText(extractedText)
                    }
                    progressBar.visibility = View.GONE
                    takePictureButton.visibility = View.VISIBLE
                }
                tempFile.delete()
            } catch (e: Exception) { Log.e("OCR", "Error: ${e.message}") }
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
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
}