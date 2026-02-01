package com.mattev02.expirationdate.itemlist.database

import android.database.Cursor
import com.mattev02.expirationdate.itemlist.Item
import java.time.LocalDate
import androidx.core.database.sqlite.transaction

object ItemDao {

    private val db = Database.connection

    fun create(item: Item, listId: Int) {
        val sql = """
            INSERT OR IGNORE INTO ${Database.TABLE_ITEM} 
            (id, list_id, name, quantity, taken, is_expirable, expiration_date) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val statement = db.compileStatement(sql)

        db.transaction {
            try {
                statement.bindLong(
                    1,
                    item.id.toLong()
                )
                statement.bindLong(2, listId.toLong())
                statement.bindString(3, item.name)
                statement.bindLong(4, item.quantity.toLong())
                statement.bindLong(5, if (item.taken) 1 else 0)
                statement.bindLong(6, if (item.isExpirable) 1 else 0)

                if (item.expirationDate != null) {
                    statement.bindString(
                        7,
                        item.expirationDate.toString() // ISO-8601 (YYYY-MM-DD)
                    )
                } else {
                    statement.bindNull(7)
                }

                statement.executeInsert()
            } finally {
                statement.close()
            }
        }
    }

    fun readItemsByListId(listId: Long): MutableList<ItemDatabase> {
        val items = mutableListOf<ItemDatabase>()
        val sql = "SELECT * FROM ${Database.TABLE_ITEM} WHERE list_id = ?"

        val cursor: Cursor = db.rawQuery(sql, arrayOf(listId.toString()))

        try {
            if (cursor.moveToFirst()) {
                do {
                    items.add(readItemFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        return items
    }

    private fun readItemFromCursor(cursor: Cursor): ItemDatabase {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val listId = cursor.getInt(cursor.getColumnIndexOrThrow("list_id"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        val quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))
        val taken = cursor.getInt(cursor.getColumnIndexOrThrow("taken")) == 1
        val isExpirable = cursor.getInt(cursor.getColumnIndexOrThrow("is_expirable")) == 1

        val dateStr = cursor.getString(cursor.getColumnIndexOrThrow("expiration_date"))
        val expirationDate = if (dateStr != null) LocalDate.parse(dateStr) else null

        val item = Item(name, quantity, taken, isExpirable, expirationDate)
        item.id = id.toInt()
        return ItemDatabase(item, listId)
    }

    fun readItemById(itemId: Long): ItemDatabase? {
        var result: ItemDatabase? = null
        val sql = "SELECT * FROM ${Database.TABLE_ITEM} WHERE id = ?"

        val cursor: Cursor = db.rawQuery(sql, arrayOf(itemId.toString()))

        try {
            if (cursor.moveToFirst()) {
                result = readItemFromCursor(cursor)
            }
        } finally {
            cursor.close()
        }

        return result
    }

    @Suppress("unused")
    fun readAllItems(): List<ItemDatabase> {
        val result = mutableListOf<ItemDatabase>()
        val sql = "SELECT * FROM ${Database.TABLE_ITEM}"

        val cursor: Cursor = db.rawQuery(sql, arrayOf())

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(readItemFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }

        return result
    }

    fun readAllIds(): List<Int> {
        val result = mutableListOf<Int>()
        val sql = "SELECT id FROM ${Database.TABLE_ITEM}"
        val cursor: Cursor = db.rawQuery(sql, arrayOf())

        try {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id")).toInt()
                    result.add(id)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }

        return result
    }

    fun update(item: Item) {
        val sql = """
            UPDATE ${Database.TABLE_ITEM} 
            SET name = ?, quantity = ?, taken = ?, is_expirable = ?, expiration_date = ? 
            WHERE id = ?
        """.trimIndent()

        val statement = db.compileStatement(sql)

        try {
            statement.bindString(1, item.name)
            statement.bindLong(2, item.quantity.toLong())
            statement.bindLong(3, if (item.taken) 1 else 0)
            statement.bindLong(4, if (item.isExpirable) 1 else 0)

            if (item.expirationDate != null) {
                statement.bindString(5, item.expirationDate.toString())
            } else {
                statement.bindNull(5)
            }

            statement.bindLong(6, item.id.toLong())

            statement.executeUpdateDelete()
        } finally {
            statement.close()
        }
    }

    fun delete(item: Item) {
        val sql = "DELETE FROM ${Database.TABLE_ITEM} WHERE id = ?"
        val statement = db.compileStatement(sql)
        try {
            statement.bindLong(1, item.id.toLong())
            statement.executeUpdateDelete()
        } finally {
            statement.close()
        }
    }
}