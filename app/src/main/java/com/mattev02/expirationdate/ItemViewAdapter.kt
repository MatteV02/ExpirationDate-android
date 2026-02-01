package com.mattev02.expirationdate

import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.itemlist.database.ItemDatabase
import com.mattev02.expirationdate.itemlist.database.ItemListDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * This class is used from the RecycleView in ListDetailActivity for showing a list of items inside
 * a certain list.
 */
class ItemViewAdapter(
    var dataSet: MutableList<Item>,
    var itemListId: Int,
    val lifecycleScope: CoroutineScope
) :
    RecyclerView.Adapter<ItemViewAdapter.ViewHolder>() {

    /**
     * ViewHolder customization for showing an item.
     */
    class ViewHolder(
        view: View,
        private val itemListId: Int,
        val lifecycleScope: CoroutineScope
    ) : RecyclerView.ViewHolder(view) {
        // Define click listener for the ViewHolder's View
        val takeItemRadio: CheckBox = view.findViewById(R.id.take_item_radio)
        val itemName: EditText = view.findViewById(R.id.item_name)
        val quantity: TextInputEditText = view.findViewById(R.id.quantity)
        val quantityLayout : TextInputLayout = view.findViewById(R.id.quantity_text_layout)
        val expirationDateLabel : TextView = view.findViewById(R.id.expiration_date_label)
        val editButton: ImageButton = view.findViewById(R.id.edit_item_button)

        var item: Item? = null

        init {
            takeItemRadio.isChecked = item?.taken ?: false
            takeItemRadio.setOnCheckedChangeListener { view, status ->
                item?.taken = status
                if (status) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        // create a new item inside the pantry
                        val pantryItem =
                            ItemListDao.read(42)?.list?.find { it.name == item?.name || it.id == item?.id }
                        if (pantryItem == null) {
                            item?.let {
                                ItemDatabase(
                                    Item(
                                        it.name,
                                        it.quantity,
                                        it.taken,
                                        it.isExpirable,
                                        it.expirationDate
                                    ), 42
                                )
                            }
                        }
                    }
                }
                // enable/disable fields
                if (status && itemListId != 42) {
                    quantity.inputType = InputType.TYPE_NULL
                    itemName.inputType = InputType.TYPE_NULL
                } else {
                    quantity.inputType = InputType.TYPE_CLASS_NUMBER
                    itemName.inputType = InputType.TYPE_CLASS_TEXT
                }
            }
            takeItemRadio.setOnClickListener { view ->
                if (item?.taken ?: false) {
                    Toast.makeText(view.context, R.string.message_add_item, Toast.LENGTH_LONG).show()
                    editButton.callOnClick()
                }
            }
            if (itemListId == 42) {
                takeItemRadio.visibility = View.GONE
            }

            itemName.doOnTextChanged { text, start, before, count ->
                item?.name = text.toString()
            }

            quantity.doOnTextChanged { text, start, before, count ->
                try {
                    item?.quantity = text.toString().toInt()
                } catch (_ : NumberFormatException) {}
            }

            editButton.setOnClickListener {
                var itemId = item?.id ?: 0
                var itemListId = itemListId
                if (item?.taken ?: false) {
                    itemId = ItemListDao.read(42)?.list?.find { i -> i.name == item?.name }?.id ?: itemId
                    itemListId = 42
                }
                val intent = Intent(view.context, ItemDetailActivity::class.java)
                intent.putExtra("itemId", itemId)
                intent.putExtra("itemListId", itemListId)
                view.context.startActivity(intent)
            }

            // show/hide quantity and expirationDate fields
            if (itemListId == 42) {
                quantityLayout.visibility = View.GONE
            } else {
                expirationDateLabel.visibility = View.GONE
            }

            if ((item?.taken ?: false) && itemListId != 42) {
                // disable fields
                quantity.inputType = InputType.TYPE_NULL
                itemName.inputType = InputType.TYPE_NULL
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_view, viewGroup, false)

        return ViewHolder(view, itemListId, lifecycleScope)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]
        viewHolder.item = item
        viewHolder.itemName.setText(item.name)
        viewHolder.quantity.setText(item.quantity.toString())
        viewHolder.expirationDateLabel.text = item.expirationDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        viewHolder.takeItemRadio.isChecked = item.taken
        if (!item.isExpirable) {
            viewHolder.expirationDateLabel.visibility = View.GONE
        } else {
            viewHolder.expirationDateLabel.visibility = View.VISIBLE
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}