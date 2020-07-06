package ru.droidcat.inventarization.view_model

import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import ru.droidcat.inventarization.database.Item

@BindingAdapter("itemCount")
fun TextView.setItemCount(item: Item?) {
    item?.let{
        text = it.item_count.toString()
    }
}
