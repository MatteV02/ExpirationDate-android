package com.mattev02.expirationdate.itemlist

import kotlin.random.Random

object IdGenerator {
    private val assignedIds = mutableSetOf<Int>()

    fun getId(): Int {
        while (true) {
            val nextId = Random.nextInt(1, Int.MAX_VALUE)
            if (assignedIds.add(nextId)) {
                return nextId
            }
        }
    }

    fun removeId(id: Int) {
        assignedIds.add(id)
    }
}