package ru.droidcat.inventarization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.droidcat.inventarization.view_model.ItemListener
import ru.droidcat.inventarization.view_model.ItemsListAdapter
import ru.droidcat.inventarization.view_model.ItemsViewModel

class ItemsList: Fragment() {

    private lateinit var itemsViewModel: ItemsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.items_list, container, false)

        recyclerView = view.findViewById(R.id.items_list)
        adapter = ItemsListAdapter(itemListener)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)
        itemsViewModel.items.observe(viewLifecycleOwner, Observer { items ->
            items?.let { adapter.submitList(it) }
        })

        adapter.setViewModel(itemsViewModel)

        //exitTransition = Hold()

        return view
    }

    private val itemListener = ItemListener { item, image ->
        val extras = FragmentNavigatorExtras(image to image.transitionName)
        val bundle = Bundle()
        bundle.putString("item_id", item.item_id)
        bundle.putString("image", image.transitionName)
        findNavController().navigate(R.id.action_itemsList_to_itemView, bundle, null, extras)
    }
}