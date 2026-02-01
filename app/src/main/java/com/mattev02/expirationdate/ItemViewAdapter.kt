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
import java.time.format.DateTimeFormatter


class ItemViewAdapter(
    var dataSet: MutableList<Item>,
    var itemListId: Int
) :
    RecyclerView.Adapter<ItemViewAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(
        view: View,
        private val itemListId: Int
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
                    Thread {
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
                    }.start()
                }
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

            if (itemListId == 42) {
                quantityLayout.visibility = View.GONE
            } else {
                expirationDateLabel.visibility = View.GONE
            }

            if ((item?.taken ?: false) && itemListId != 42) {
                quantity.inputType = InputType.TYPE_NULL
                itemName.inputType = InputType.TYPE_NULL
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_view, viewGroup, false)

        return ViewHolder(view, itemListId)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val item = dataSet[position]
        viewHolder.item = item
        viewHolder.itemName.setText(item.name)
        viewHolder.quantity.setText(item.quantity.toString())
        viewHolder.expirationDateLabel.text = item.expirationDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        viewHolder.takeItemRadio.isChecked = item.taken
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}