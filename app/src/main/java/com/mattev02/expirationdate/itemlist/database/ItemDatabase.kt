package com.mattev02.expirationdate.itemlist.database

import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.itemlist.ItemDecorator

class ItemDatabase(
    item: Item,
    listId: Int
) : ItemDecorator(item) {

    init {
        ItemDao.create(item, listId)
    }

    override fun onQuantityChanged() {
        ItemDao.update(item)
    }

    override fun onExpirationDateChanged() {
        ItemDao.update(item)
    }

    override fun onIsExpirableChanged() {
        ItemDao.update(item)
    }

    override fun onTakenChanged() {
        ItemDao.update(item)
    }

    override fun onNameChanged() {
        ItemDao.update(item)
    }

    fun delete() {
        ItemDao.delete(item)
    }
}