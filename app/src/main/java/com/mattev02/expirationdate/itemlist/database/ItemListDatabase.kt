package com.mattev02.expirationdate.itemlist.database

import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.itemlist.ItemList
import com.mattev02.expirationdate.itemlist.ItemListDecorator

class ItemListDatabase(
    itemList: ItemList
) : ItemListDecorator(itemList) {

    init {
        ItemListDao.create(itemList)
    }

    override fun onNameChanged() {
        ItemListDao.update(itemList)
    }

    override fun addItem(item: Item) {
        super.addItem(ItemDatabase(item, id))
    }

    override fun removeItem(item: Item) {
        super.removeItem(item)
        if (item is ItemDatabase) {
            item.delete()
        }
    }

    override fun removeAll() {
        list.forEach { item ->
            if (item is ItemDatabase) {
                item.delete()
            }
        }
        super.removeAll()
    }
}