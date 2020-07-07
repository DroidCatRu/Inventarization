package ru.droidcat.inventarization.database

import androidx.lifecycle.LiveData

class ItemsRepository(private val itemDAO: ItemDAO) {

    val items: LiveData<List<Item>> = itemDAO.getAllItems()

    fun getItem(item_id: String) = itemDAO.getItem(item_id)
    suspend fun insertItem(item: Item) = itemDAO.insert(item)
    suspend fun deleteItem(item_id: String) = itemDAO.deleteItem(item_id)
    suspend fun renameItem(item_name: String, item_id: String) = itemDAO.renameItem(item_name, item_id)
    suspend fun changeItemCount(item_count: Long, item_id: String) = itemDAO.changeItemCount(item_count, item_id)
    suspend fun changeItemImgUrl(item_img_url: String, item_id: String) = itemDAO.changeItemImgUrl(item_img_url, item_id)
}