package ru.droidcat.inventarization.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ItemDAO {
    @Query("SELECT * FROM items_table ORDER BY NOT(item_count = 0), LOWER(item_name), item_id ASC")
    fun getAllItems(): LiveData<List<Item>>

    @Query("SELECT * FROM items_table WHERE item_id = :item_id")
    fun getItem(item_id: String): LiveData<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Query("DELETE FROM items_table WHERE item_id = :item_id")
    suspend fun deleteItem(item_id: String)

    @Query("UPDATE items_table SET item_name = :item_name WHERE item_id = :item_id")
    suspend fun renameItem(item_name: String, item_id: String)

    @Query("UPDATE items_table SET item_count = :item_count WHERE item_id = :item_id")
    suspend fun changeItemCount(item_count: Long, item_id: String)

    @Query("UPDATE items_table SET item_img_url = :item_img_url WHERE item_id = :item_id")
    suspend fun changeItemImgUrl(item_img_url: String, item_id: String)
}