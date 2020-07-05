package ru.droidcat.inventarization

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.nav_container.*
import ru.droidcat.inventarization.databinding.NavContainerBinding
import ru.droidcat.inventarization.view_model.ItemsViewModel

class NavContainer : AppCompatActivity() {

    private lateinit var binding: NavContainerBinding
    private lateinit var itemsViewModel: ItemsViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.nav_container)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        binding.title = getString(R.string.items_screen_title)
        binding.executePendingBindings()

        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar!!.setDisplayHomeAsUpEnabled(destination.id != R.id.itemsList)

            when(destination.id) {
                R.id.itemsList -> {
                    binding.title = getString(R.string.items_screen_title)
                    add_item_button.show()
                }
                R.id.itemView -> {
                    binding.title = getString(R.string.item_view_title)
                    add_item_button.hide()
                }
                else -> {
                    binding.title = getString(R.string.app_name)
                    add_item_button.hide()
                }
            }

            binding.executePendingBindings()
        }
    }

    fun addItemDialog(view: View?) {
        //itemsViewModel.insert("New item")
        navController.navigate(R.id.addItem)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
