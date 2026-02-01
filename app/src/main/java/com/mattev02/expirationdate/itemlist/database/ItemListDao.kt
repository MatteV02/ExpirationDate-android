package com.mattev02.expirationdate.itemlist.database

import android.database.Cursor
import androidx.core.database.sqlite.transaction
import com.mattev02.expirationdate.itemlist.ItemList

object ItemListDao {

    private val db = Database.connection

        fun create(itemList: ItemList) {
        val sql = "INSERT OR IGNORE INTO ${Database.TABLE_ITEM_LIST} (id, name) VALUES (?, ?)"
        val statement = db.compileStatement(sql)

        db.transaction {
            try {
                statement.bindLong(1, itemList.id.toLong())
                statement.bindString(2, itemList.name)
                statement.executeInsert()

                itemList.list = itemList.list.map { item ->
                    ItemDatabase(item, itemList.id)
                }.toMutableList()

            } finally {
                statement.close()
            }
        }
    }

    fun read(id: Long): ItemListDatabase? {
        val sql = "SELECT * FROM ${Database.TABLE_ITEM_LIST} WHERE id = ?"
        val cursor: Cursor = db.rawQuery(sql, arrayOf(id.toString()))

        var itemList: ItemList? = null

        try {
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

                itemList = ItemList(name)
                itemList.id = id.toInt()

                itemList.list = ItemDao.readItemsByListId(id).toMutableList()
            }
        } finally {
            cursor.close()
        }

        return if (itemList != null) {
            ItemListDatabase(itemList)
        } else {
            null
        }
    }

   fun readAll(): List<ItemListDatabase> {
        val result = mutableListOf<ItemListDatabase>()
        val sql = "SELECT * FROM ${Database.TABLE_ITEM_LIST}"
        val cursor: Cursor = db.rawQuery(sql, null)

        try {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

                    val list = ItemList(name)
                    list.id = id.toInt()

                    list.list = ItemDao.readItemsByListId(id).toMutableList()

                    result.add(ItemListDatabase(list))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        return result
    }

    fun readAllIds(): List<Int> {
        val result = mutableListOf<Int>()
        val sql = "SELECT id FROM ${Database.TABLE_ITEM_LIST}"
        val cursor: Cursor = db.rawQuery(sql, null)

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

    fun update(itemList: ItemList) {
        val sql = "UPDATE ${Database.TABLE_ITEM_LIST} SET name = ? WHERE id = ?"
        val statement = db.compileStatement(sql)
        try {
            statement.bindString(1, itemList.name)
            statement.bindLong(2, itemList.id.toLong())
            statement.executeUpdateDelete()
        } finally {
            statement.close()
        }
    }

    fun delete(itemList: ItemList) {
        val sql = "DELETE FROM ${Database.TABLE_ITEM_LIST} WHERE id = ?"
        val statement = db.compileStatement(sql)
        try {
            statement.bindLong(1, itemList.id.toLong())
            statement.executeUpdateDelete()
        } finally {
            statement.close()
        }
    }
}