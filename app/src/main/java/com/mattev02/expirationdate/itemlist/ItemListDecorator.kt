package com.mattev02.expirationdate.itemlist

abstract class ItemListDecorator(
    val itemList: ItemList
) : ItemList() {

    override var name: String
        get() = itemList.name
        set(value) {
            itemList.name = value
            onNameChanged()
        }

    override var id: Int
        get() = itemList.id
        set(value) {
            itemList.id = value
        }
    override var list: MutableList<Item>
        get() = itemList.list
        set(value) {
            itemList.list = value
        }

    abstract fun onNameChanged()
}