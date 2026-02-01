package com.mattev02.expirationdate

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.itemlist.ItemList
import com.mattev02.expirationdate.itemlist.database.Database
import com.mattev02.expirationdate.itemlist.database.ItemListDao
import com.mattev02.expirationdate.notification.NotificationHelper
import com.mattev02.expirationdate.settings.SettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ListDetailActivity : AppCompatActivity() {
    var itemList: ItemList = ItemList("")
    var adapter = ItemViewAdapter(itemList.list, itemList.id)
    lateinit var toolbar: MaterialToolbar

    var listId : Int? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listId = intent.extras?.getInt("listId")

        val addButton = findViewById<FloatingActionButton>(R.id.add_item_button)
        addButton.setOnClickListener { view -> addItem() }

        val listView = findViewById<RecyclerView>(R.id.list_view)
        listView.layoutManager = LinearLayoutManager(applicationContext)
        listView.adapter = adapter

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { view -> finish() }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.rename_list -> renameList(toolbar)
                R.id.remove_list -> removeList(toolbar)
                R.id.clear_list -> {
                    if (listId == 42) {
                        itemList.list.forEach { item ->
                            NotificationHelper.cancelNotification(this, item)
                        }
                    }
                    itemList.removeAll()
                    adapter.notifyDataSetChanged()
                }
            }
            true
        }
        if (listId == 42) {
            toolbar.menu.findItem(R.id.rename_list).isVisible = false
            toolbar.menu.findItem(R.id.remove_list).isVisible = false
        }

        lifecycleScope.launch(Dispatchers.IO) {
            Database.init(this@ListDetailActivity)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            val localListId = listId
            if (localListId != null) {
                itemList = ItemListDao.read(localListId.toLong()) ?: itemList
                lifecycleScope.launch(Dispatchers.Main) {
                    toolbar.title = if (listId != 42) itemList.name else getString(R.string.pantry)
                    adapter.dataSet = itemList.list
                    adapter.itemListId = itemList.id
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun addItem() {
        val newItem = Item(expirationDate = LocalDate.now().plusDays(SettingsHelper.getDefaultExpirationDay(this).toLong()))
        if (itemList.id == 42) {
            newItem.taken = true
        }
        itemList.addItem(newItem)
        adapter.notifyItemInserted(itemList.list.size - 1)
    }

    fun renameList(view: View) {
        val context = view.context
        val alertDialogBuilder = AlertDialog.Builder(context)
        val editText = EditText(context)
        editText.setText(itemList.name)
        editText.hint = context.getString(R.string.rename_list_hint)
        alertDialogBuilder.setView(editText)
        alertDialogBuilder.setCancelable(false).setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
            itemList.name = editText.text.toString()
            toolbar.title = itemList.name
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    fun removeList(view: View) {
        val context = view.context
        val alertDialogBuilder = AlertDialog.Builder(context)
        val textLabel = TextView(context)
        textLabel.text = context.getString(R.string.remove_list)
        alertDialogBuilder.setView(textLabel)
        alertDialogBuilder.setCancelable(false).setPositiveButton(context.getString(R.string.yes)) { dialog, which ->
            ItemListDao.delete(itemList)
            finish()
        } .setNegativeButton(context.getString(R.string.no)) { dialog, which -> }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}