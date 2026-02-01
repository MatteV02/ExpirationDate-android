package com.mattev02.expirationdate.itemlist

import java.time.LocalDate

abstract class ItemDecorator(
    val item: Item
) : Item() {
    override var name: String
        get() = item.name
        set(value) {
            item.name = value
            onNameChanged()
        }

    override var taken: Boolean
        get() = item.taken
        set(value) {
            item.taken = value
            onTakenChanged()
        }

    override var isExpirable: Boolean
        get() = item.isExpirable
        set(value) {
            item.isExpirable = value
            onIsExpirableChanged()
        }

    override var expirationDate: LocalDate?
        get() = item.expirationDate
        set(value) {
            item.expirationDate = value
            onExpirationDateChanged()
        }

    override var id: Int
        get() = item.id
        set(value) {
            item.id = value
        }

    override var quantity: Int
        get() = item.quantity
        set(value) {
            item.quantity = value
            onQuantityChanged()
        }

    abstract fun onQuantityChanged()
    abstract fun onExpirationDateChanged()
    abstract fun onIsExpirableChanged()
    abstract fun onTakenChanged()
    abstract fun onNameChanged()

}