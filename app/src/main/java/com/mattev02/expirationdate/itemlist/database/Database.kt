package com.mattev02.expirationdate.itemlist.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mattev02.expirationdate.itemlist.IdGenerator

object Database {
    private const val DB_NAME = "expiration_date.db"
    private const val DB_VERSION = 1

    const val TABLE_ITEM_LIST = "item_list"
    const val TABLE_ITEM = "item"

    private var dbHelper: DatabaseHelper? = null

    val connection: SQLiteDatabase
        get() = dbHelper?.writableDatabase
            ?: throw IllegalStateException("Database not initialized. Call Database.init(context) before use.")

    fun init(context: Context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper(context.applicationContext)
            val itemIds = ItemDao.readAllIds()
            itemIds.forEach { id ->
                IdGenerator.removeId(id)
            }
            val itemListIds = ItemListDao.readAllIds()
            itemListIds.forEach { id ->
                IdGenerator.removeId(id)
            }
        }
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            val createListTable = """
                CREATE TABLE $TABLE_ITEM_LIST (
                    id INTEGER PRIMARY KEY, 
                    name TEXT NOT NULL
                )
            """.trimIndent()

            val createItemTable = """
                CREATE TABLE $TABLE_ITEM (
                    id INTEGER PRIMARY KEY,
                    list_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    taken INTEGER NOT NULL,
                    is_expirable INTEGER NOT NULL,
                    expiration_date TEXT,
                    FOREIGN KEY(list_id) REFERENCES $TABLE_ITEM_LIST(id) ON DELETE CASCADE
                )
            """.trimIndent()

            db.execSQL(createListTable)
            db.execSQL(createItemTable)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEM")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEM_LIST")
            onCreate(db)
        }
    }
}