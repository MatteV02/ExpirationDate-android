package com.mattev02.expirationdate.itemlist

import java.time.LocalDate

open class Item(
    open var name: String = "",
    quantity: Int = 1,
    open var taken: Boolean = false,
    open var isExpirable: Boolean = true,
    open var expirationDate: LocalDate? = LocalDate.now()
) {
    open var id = IdGenerator.getId()
    open var quantity: Int = quantity
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Item quantity should not be negative")
            }
            field = value
        }

    override fun toString(): String {
        return "Item(name='$name', quantity=$quantity, taken=$taken, isExpirable=$isExpirable, expirationDate=$expirationDate)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Item

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}