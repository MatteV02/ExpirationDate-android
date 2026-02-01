package com.mattev02.expirationdate.itemlist

open class ItemList(
    open var name: String = ""
) {

    open var id = IdGenerator.getId()

    open var list = mutableListOf<Item>()

    open fun addItem(item: Item) { list.add(item) }

    @Suppress("unused")
    open fun updateItem(item: Item) {
        val itemIndex = list.indexOf(item)

        if (itemIndex < 0) {
            throw ItemNotFoundException("$item not found in list ${this.name}")
        }

        list[itemIndex] = item
    }

    open fun removeItem(item: Item) {
        val ret = list.remove(item)
        if (!ret) {
            throw ItemNotFoundException("$item not found in list ${this.name}")
        }
    }

    open fun removeAll() {
        list.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemList

        if (id != other.id) return false
        if (name != other.name) return false
        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + list.hashCode()
        return result
    }
}