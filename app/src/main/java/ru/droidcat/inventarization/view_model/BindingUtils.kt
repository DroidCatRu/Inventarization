package ru.droidcat.inventarization.view_model

import android.widget.TextView
import androidx.databinding.BindingAdapter
import ru.droidcat.inventarization.database.Item

@BindingAdapter("itemCount")
fun TextView.setItemCount(item: Item?) {
    item?.let{
        text = it.item_count.toString()
    }
}

@BindingAdapter("itemId")
fun TextView.setItemId(item: Item?) {
    item?.let{
        text = "Item id: ${it.item_id}"
    }
}

@BindingAdapter("itemCount")
fun TextView.setItemCountFromLong(count: Long?) {
    text = count?.toString()
}