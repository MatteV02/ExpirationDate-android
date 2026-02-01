package com.mattev02.expirationdate.itemlist

open class ListException(override val message: String? = null) : RuntimeException()

class ItemNotFoundException(override val message: String? = null) : ListException()
