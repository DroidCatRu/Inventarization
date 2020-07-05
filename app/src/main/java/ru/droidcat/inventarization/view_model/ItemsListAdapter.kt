package ru.droidcat.inventarization.view_model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.items_list_item.view.*
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.databinding.ItemsListItemBinding

class ItemsListAdapter(
    private val clickListener: ItemListener
): ListAdapter<Item, ItemsListAdapter.ViewHolder>(ItemsDiffCallback()) {

    private lateinit var viewModel: ItemsViewModel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position),
            clickListener,
            holder.itemView.start_item_image)
    }

    class ViewHolder private constructor(
        val binding: ItemsListItemBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item, clickListener: ItemListener, image: View) {
            binding.item = item
            binding.image = image
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemsListItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    fun setViewModel(itemsViewModel: ItemsViewModel) {
        this.viewModel = itemsViewModel
    }

}

class ItemListener(val clickListener: (item: Item, image: View) -> Unit) {
    fun onClick(item: Item, image: View) = clickListener(item, image)
}

class ItemsDiffCallback: DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.item_id == newItem.item_id
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}