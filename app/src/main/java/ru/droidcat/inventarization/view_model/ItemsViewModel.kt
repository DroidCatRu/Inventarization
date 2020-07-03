package ru.droidcat.inventarization.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.droidcat.inventarization.database.AppDatabase
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.database.ItemsRepository
import java.text.SimpleDateFormat
import java.util.*

class ItemsViewModel(application: Application): AndroidViewModel(application) {

    private val itemDAO = AppDatabase.getDatabase(application, viewModelScope).itemDao()
    private val repository: ItemsRepository = ItemsRepository(itemDAO)
    val items: LiveData<List<Item>> = repository.items

    fun insert(name: String) = viewModelScope.launch {
        val sdf = SimpleDateFormat("dd/M/yyyy HH:mm:ss:SSS", Locale.ENGLISH)
        val currentDate = sdf.format(Date())
        val item = Item(currentDate, name, 0, "")
        repository.insertItem(item)
    }

    fun insert(name: String, count: Long) = viewModelScope.launch {
        val sdf = SimpleDateFormat("dd/M/yyyy HH:mm:ss:SSS", Locale.ENGLISH)
        val currentDate = sdf.format(Date())
        val item = Item(currentDate, name, count, "")
        repository.insertItem(item)
    }

    fun delete(item_id: String) = viewModelScope.launch {
        repository.deleteItem(item_id)
    }

    fun renameItem(item_name: String, item_id: String) = viewModelScope.launch {
        repository.renameItem(item_name, item_id)
    }

    fun changeItemCount(item_count: Long, item_id: String) = viewModelScope.launch {
        if(item_count < 0) {
            repository.changeItemCount(0, item_id)
        }
        else {
            repository.changeItemCount(item_count, item_id)
        }
    }

    fun changeItemImgUrl(item_img_url: String, item_id: String) = viewModelScope.launch {
        repository.changeItemImgUrl(item_img_url, item_id)
    }
}