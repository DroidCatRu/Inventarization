package ru.droidcat.inventarization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.droidcat.inventarization.database.Item
import ru.droidcat.inventarization.view_model.ItemListener
import ru.droidcat.inventarization.view_model.ItemsListAdapter
import ru.droidcat.inventarization.view_model.ItemsViewModel

class ItemsList: Fragment() {

    private lateinit var itemsViewModel: ItemsViewModel
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: ItemsListAdapter
    lateinit var onItemCallback: OnItemCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.items_list, container, false)

        //onItemCallback = arguments.get("onItemCallback") as OnItemCallback

        recyclerView = view.findViewById(R.id.items_list)
        adapter = ItemsListAdapter(itemListener)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)
        itemsViewModel.items.observe(viewLifecycleOwner, Observer { items ->
            items?.let { adapter.setItems(it) }
        })

        adapter.setViewModel(itemsViewModel)

        return view
    }

    private val itemListener = ItemListener { item ->
        //onItemCallback?.onClick(item)
    }
}

class OnItemCallback(val clickListener: (item: Item) -> Unit) {
    fun onClick(item: Item) = clickListener(item)
}