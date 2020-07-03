package ru.droidcat.inventarization.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items_table")
data class Item(
    @PrimaryKey
    @ColumnInfo(name = "item_id") val item_id: String,
    @ColumnInfo(name = "item_name") val item_name: String,
    @ColumnInfo(name = "item_count") val item_count: Long,
    @ColumnInfo(name = "item_img_url") val item_img_url: String)