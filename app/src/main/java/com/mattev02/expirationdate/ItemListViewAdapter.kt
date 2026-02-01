package com.mattev02.expirationdate

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mattev02.expirationdate.itemlist.ItemList
import com.mattev02.expirationdate.notification.NotificationHelper


class ItemListViewAdapter(
    var dataSet: MutableList<ItemList>,
    private val listRenamer: (ItemList, Context) -> Unit,
    private val listRemover: (ItemList, Context) -> Unit
) :
    RecyclerView.Adapter<ItemListViewAdapter.ViewHolder>() {

    class ViewHolder(
        view: View,
        private val listRenamer: (ItemList, Context) -> Unit,
        private val listRemover: (ItemList, Context) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        val listName: TextView = view.findViewById(R.id.list_name)
        val infoButton: ImageButton = view.findViewById(R.id.info_button)
        lateinit var itemList: ItemList

        init {
            view.setOnClickListener { view ->
                val intent = Intent(view.context, ListDetailActivity::class.java)
                intent.putExtra("listId", itemList.id)
                view.context.startActivity(intent)
            }

            infoButton.setOnClickListener {
                val popupMenu = PopupMenu(view.context, infoButton)
                // Inflating popup menu from popup_menu.xml file
                popupMenu.menuInflater.inflate(R.menu.item_list_menu, popupMenu.menu)

                // Handling menu item click events
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.rename_list -> listRenamer(itemList, view.context)
                        R.id.remove_list -> if (itemList.id != 42) listRemover(itemList, view.context) else Toast.makeText(view.context, R.string.error_delete_pantry, Toast.LENGTH_LONG).show()
                        R.id.clear_list -> {
                            if (itemList.id == 42) {
                                itemList.list.forEach { item ->
                                    NotificationHelper.cancelNotification(view.context, item)
                                }
                            }
                            itemList.removeAll()
                        }
                    }
                    true
                }

                // Showing the popup menu
                popupMenu.show()
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_list_view, viewGroup, false)

        return ViewHolder(view, listRenamer, listRemover)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val item = dataSet[position]
        viewHolder.itemList = item
        viewHolder.listName.text = item.name
    }

    override fun getItemCount() = dataSet.size

}