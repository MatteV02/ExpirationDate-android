package com.mattev02.expirationdate

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mattev02.expirationdate.itemlist.ItemList


/**
 * This class is used from the RecycleView in MainActivity for showing a list of lists.
 */
class ItemListViewAdapter(
    var dataSet: MutableList<ItemList>,
    private val listRenamer: (ItemList, Context) -> Unit,
    private val listRemover: (ItemList, Context) -> Unit
) :
    RecyclerView.Adapter<ItemListViewAdapter.ViewHolder>() {

    /**
     * ViewHolder customization for showing a list.
     */
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
                            R.id.remove_list -> listRemover(itemList, view.context)
                            R.id.clear_list -> itemList.removeAll()
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
        val item = dataSet[position]
        viewHolder.itemList = item
        viewHolder.listName.text = item.name
    }

    override fun getItemCount() = dataSet.size

}