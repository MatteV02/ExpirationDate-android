package com.mattev02.expirationdate.itemlist

import org.junit.Test
import org.junit.Assert.*

class IdGeneratorTest {
    @Test
    fun getId() {
        val attempts = mutableSetOf<Int>()
        for (i in 1..100) {
            val id = IdGenerator.getId()
            val ret = attempts.add(id)
            assertTrue(ret)
        }
    }

}