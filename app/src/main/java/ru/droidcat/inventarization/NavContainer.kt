package ru.droidcat.inventarization

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.nav_container.*
import ru.droidcat.inventarization.databinding.NavContainerBinding
import ru.droidcat.inventarization.view_model.ItemsViewModel

class NavContainer : AppCompatActivity() {

    lateinit var binding: NavContainerBinding
    private lateinit var itemsViewModel: ItemsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.nav_container)
        setSupportActionBar(toolbar)

        binding.title = "Main screen"
        binding.executePendingBindings()

        itemsViewModel = ViewModelProvider(this).get(ItemsViewModel::class.java)
    }

    fun addItemDialog(view: View?) {
        Log.d("Nav Container", "fab clicked!")
        binding.title = "Fab clicked & replaced"
        binding.executePendingBindings()

        itemsViewModel.insert("New item")
    }
    
}
