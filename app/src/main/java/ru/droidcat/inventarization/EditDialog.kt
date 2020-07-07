package ru.droidcat.inventarization

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ru.droidcat.inventarization.databinding.EditDialogBinding

class EditDialog(
    private val text: String,
    private val quantity: Long,
    private val editDialogCallback: EditDialogCallback
): BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: EditDialogBinding = DataBindingUtil.inflate(inflater, R.layout.edit_dialog, container, false)
        val view = binding.root

        val inputNameText = view.findViewById<TextInputEditText>(R.id.item_name_textinputedittext)
        val inputNameLayout = view.findViewById<TextInputLayout>(R.id.item_name_textinputlayout)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirm_button)

        val inputCountText = view.findViewById<TextInputEditText>(R.id.item_count_textinputedittext)
        val inputCountLayout = view.findViewById<TextInputLayout>(R.id.item_count_textinputlayout)

        inputNameText.append(text)
        inputCountText.append(quantity.toString())

        inputNameText.doAfterTextChanged {
            inputNameLayout.error = null
        }
        inputCountText.doAfterTextChanged {
            inputCountLayout.error = null
        }

        confirmButton.setOnClickListener {
            var allData = true
            val text = inputNameLayout.editText?.text.toString()
            val quantity = inputCountLayout.editText?.text.toString()

            if(text.isEmpty()) {
                inputNameLayout.error = "Item name is empty"
                allData = false
            }

            if(quantity.isEmpty()) {
                inputCountLayout.error = "Item quantity is empty"
                allData = false
            }

            if(allData) {
                editDialogCallback.onResult(text, quantity.toLong())
                dismiss()
            }
        }
        
        return view
    }
}

class EditDialogCallback(val callback: (text: String?, quantity: Long?) -> Unit) {
    fun onResult(text: String?, quantity: Long?) = callback(text, quantity)
}