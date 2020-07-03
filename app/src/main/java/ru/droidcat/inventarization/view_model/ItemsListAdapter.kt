package ru.droidcat.inventarization.view_model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.databinding.ItemsListItemBinding

class ItemsListAdapter(
    private val clickListener: ItemListener
): RecyclerView.Adapter<ItemsListAdapter.ViewHolder>() {

    private lateinit var viewModel: ItemsViewModel
    private var items = emptyList<Item>()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], clickListener)
    }

    class ViewHolder private constructor(
        val binding: ItemsListItemBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item, clickListener: ItemListener) {
            binding.item = item
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

    fun setItems(items: List<Item>) {
        this.items = items
        notifyDataSetChanged()
    }
}

class ItemListener(val clickListener: (item: Item) -> Unit) {
    fun onClick(item: Item) = clickListener(item)
}