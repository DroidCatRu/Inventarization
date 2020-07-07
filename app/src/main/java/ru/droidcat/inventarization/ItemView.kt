package ru.droidcat.inventarization

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.item_view.*
import ru.droidcat.inventarization.databinding.ItemViewBinding
import ru.droidcat.inventarization.view_model.ItemsViewModel

class ItemView: Fragment() {

    private lateinit var itemsViewModel: ItemsViewModel
    private lateinit var binding: ItemViewBinding
    private var itemId = String()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        itemId = arguments?.getString("item_id")!!

        binding = ItemViewBinding.inflate(inflater, container, false)
        binding.image = arguments?.getString("image")
        binding.executePendingBindings()

        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.move_image)
        itemsViewModel.getItem(itemId).observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.item = it
                binding.executePendingBindings()

                minusButton.isActivated = (it.item_count > 0)
            }
        })
        plusButton.setOnClickListener {
            itemsViewModel.changeItemCount(
                binding.item!!.item_count+1,
                binding.item!!.item_id)
        }
        minusButton.setOnClickListener {
            itemsViewModel.changeItemCount(
                binding.item!!.item_count-1,
                binding.item!!.item_id)
        }
        delete_item_button.setOnClickListener {
            itemsViewModel.delete(binding.item!!.item_id)
            findNavController().popBackStack()
        }
        edit_item_button.setOnClickListener {
            val editDialog = EditDialog(
                binding.item!!.item_name,
                binding.item!!.item_count,
                dialogCallback)
            editDialog.show(parentFragmentManager, editDialog.tag)
        }
    }

    private val dialogCallback = EditDialogCallback { text, quantity ->
        itemsViewModel.renameItem(text!!, binding.item!!.item_id)
        itemsViewModel.changeItemCount(quantity!!, binding.item!!.item_id)
    }
}