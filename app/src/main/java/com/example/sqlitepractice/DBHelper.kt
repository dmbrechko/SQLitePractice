package com.example.sqlitepractice

import android.database.sqlite.SQLiteException

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper (context: Context, factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {


    override fun onCreate(db: SQLiteDatabase?) {
        val query = """
            CREATE TABLE $TABLE_NAME (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_NAME TEXT,
                $KEY_WEIGHT TEXT,
                $KEY_PRICE TEXT)
        """.trimIndent()
        db?.execSQL(query)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    fun addOperation(operation: Operation) {
        val values = ContentValues()
        values.put(KEY_NAME, operation.name)
        values.put(KEY_WEIGHT, operation.weight)
        values.put(KEY_PRICE, operation.price)
        val db = this.writableDatabase
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getInfo(onError: () -> Unit): MutableList<Operation> {
        val list = mutableListOf<Operation>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        try {
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(KEY_NAME)
                val amountIndex = cursor.getColumnIndexOrThrow(KEY_WEIGHT)
                val priceIndex = cursor.getColumnIndexOrThrow(KEY_PRICE)
                do {
                    val name = cursor.getString(nameIndex)
                    val amount = cursor.getInt(amountIndex)
                    val price = cursor.getInt(priceIndex)
                    val operation = Operation(name, amount, price)
                    list.add(operation)
                } while (cursor.moveToNext())
            }
        } catch (e: SQLiteException) {
            onError()
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "PERSON_DATABASE"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "person_table"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_WEIGHT = "weight"
        const val KEY_PRICE = "price"
    }
}

data class Operation(val name: String, val weight: Int, val price: Int)