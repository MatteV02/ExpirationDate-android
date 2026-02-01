package com.mattev02.expirationdate

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mattev02.expirationdate.itemlist.ItemList
import com.mattev02.expirationdate.itemlist.database.Database
import com.mattev02.expirationdate.itemlist.database.ItemDao
import com.mattev02.expirationdate.itemlist.database.ItemListDao
import com.mattev02.expirationdate.itemlist.database.ItemListDatabase
import com.mattev02.expirationdate.settings.SettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * App homepage
 */
class MainActivity : AppCompatActivity() {
    private var lists = mutableListOf<ItemList>()

    // This variable is used from the RecycleView
    private val adapter = ItemListViewAdapter(lists,
        {item, context -> renameItemList(item, context)},
        {item, context -> removeItemList(item, context)}
    )

    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val addButton = findViewById<FloatingActionButton>(R.id.add_list_button)
        addButton.setOnClickListener { view -> addList() }
        val listView: RecyclerView = findViewById(R.id.list_view)
        listView.layoutManager = LinearLayoutManager(this)
        listView.adapter = adapter

        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.app_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        pieChart = findViewById(R.id.pantry_pie_chart)
        setupPieChart()
        pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) { openPantry() }
            override fun onNothingSelected() { openPantry() }
        })

        val openPantryButton : ImageButton = findViewById(R.id.open_pantry_button)
        openPantryButton.setOnClickListener { view -> openPantry() }

        lifecycleScope.launch(Dispatchers.IO) {
            Database.init(this@MainActivity)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            lists = ItemListDao.readAll().toMutableList()
            val pantryList = lists.find { itemList -> itemList.id == 42 }
            if (pantryList == null) {
                val pantry = ItemList(getString(R.string.pantry))
                pantry.id = 42
                addList(ItemListDatabase(pantry))
            } else {
                lists.remove(pantryList)
            }
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.dataSet = lists
                adapter.notifyDataSetChanged()
            }

            val pantryItems = ItemDao.readItemsByListId(42)

            var expiredCount = 0
            var expiringSoonCount = 0
            var freshCount = 0

            val today = LocalDate.now()
            val warningThreshold = today.plusDays(SettingsHelper.getEarlyNotificationDay(this@MainActivity).toLong())

            for (item in pantryItems) {
                if (item.expirationDate != null && item.isExpirable) {
                    when {
                        item.expirationDate!!.isBefore(today) -> expiredCount++
                        item.expirationDate!!.isBefore(warningThreshold) -> expiringSoonCount++
                        else -> freshCount++
                    }
                }
            }
            lifecycleScope.launch(Dispatchers.Main) {
                updateChartData(freshCount, expiringSoonCount, expiredCount)
            }
        }
    }

    /**
     * Utility for creating a list.
     * The new list is automatically added to database.
     */
    private fun addList(itemList: ItemList? = null) {
        val newItemList: ItemList = itemList ?: ItemListDatabase(ItemList(""))
        if (itemList == null) {
            renameItemList(newItemList, this)
        }
        lists.add(newItemList)
        adapter.notifyItemChanged(lists.size-1)
    }

    /**
     * Utility for renaming a list.
     * Creates an alert dialog for prompting new name.
     */
    private fun renameItemList(itemList: ItemList, context: Context) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        val editText = EditText(context)
        editText.setText(itemList.name)
        editText.gravity = Gravity.CENTER
        editText.hint = context.getString(R.string.rename_list_hint)
        alertDialogBuilder.setView(editText)
        alertDialogBuilder.setCancelable(false).setPositiveButton(context.getString(R.string.ok)) { dialog, which ->
            itemList.name = editText.text.toString()
            ItemListDao.update(itemList)
            adapter.notifyItemChanged(lists.indexOf(itemList))
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    /**
     * Utility for list deleting.
     * It prompts the user every item list deletion.
     */
    private fun removeItemList(itemList: ItemList, context: Context) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        val textLabel = TextView(context)
        textLabel.text = context.getString(R.string.remove_list)
        textLabel.gravity = Gravity.CENTER
        textLabel.setPadding(25)
        alertDialogBuilder.setView(textLabel)
        alertDialogBuilder.setCancelable(false).setPositiveButton(context.getString(R.string.yes)) { dialog, which ->
            val index = lists.indexOf(itemList)
            lists.remove(itemList)
            ItemListDao.delete(itemList)
            adapter.notifyItemRemoved(index)
        } .setNegativeButton(context.getString(R.string.no)) { dialog, which -> }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    /**
     * This function setups the pie chart aesthetic behaviour.
     */
    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            isRotationEnabled = false

            val colorOnSurface = getColor(R.color.md_theme_onSurface)
            setEntryLabelColor(colorOnSurface)
            setEntryLabelTextSize(14f)

            setCenterTextColor(colorOnSurface)
            setCenterTextSize(16f)

            setHoleColor(getColor(R.color.md_theme_background))
            setTransparentCircleColor(R.color.md_theme_background)
            setTransparentCircleAlpha(110)

            setExtraOffsets(30f, 20f, 30f, 20f)

            holeRadius = 80f
            transparentCircleRadius = 80f

            animateY(1000)
        }
    }

    /**
     * This function fills the pie chart.
     */
    private fun updateChartData(fresh: Int, expiring: Int, expired: Int) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val totalItems = fresh + expiring + expired

        if (totalItems == 0) {
            entries.add(PieEntry(1f, ""))
            colors.add(getColor(R.color.md_theme_outlineVariant))
        } else {
            if (fresh > 0) {
                entries.add(PieEntry(fresh.toFloat(), getString(R.string.fresh)))
                colors.add(getColor(R.color.md_theme_secondaryContainer))
            }
            if (expiring > 0) {
                entries.add(PieEntry(expiring.toFloat(), getString(R.string.expiring)))
                colors.add(getColor(R.color.md_theme_tertiaryContainer))
            }
            if (expired > 0) {
                entries.add(PieEntry(expired.toFloat(), getString(R.string.expired)))
                colors.add(getColor(R.color.md_theme_errorContainer))
            }
        }
        pieChart.setTouchEnabled(true)
        pieChart.centerText = "${getString(R.string.pantry)}\n$totalItems"

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        dataSet.valueTextColor = getColor(R.color.md_theme_onSurface)
        dataSet.valueTextSize = 14f
        if (totalItems != 0) {
            dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            dataSet.valueLineColor = dataSet.valueTextColor
        }

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        dataSet.setDrawValues(totalItems > 0)
        dataSet.isHighlightEnabled = true
        dataSet.selectionShift = 0f
        dataSet.sliceSpace = 5f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    /**
     * This utility function opens the list detail view for the pantry.
     */
    private fun openPantry() {
        val intent = Intent(this, ListDetailActivity::class.java)
        intent.putExtra("listId", 42)
        startActivity(intent)
    }
}