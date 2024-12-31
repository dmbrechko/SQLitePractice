package com.example.sqlitepractice

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sqlitepractice.databinding.ActivityMainBinding
import com.example.sqlitepractice.databinding.DialogLayoutBinding
import com.example.sqlitepractice.databinding.ListItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val operations = mutableListOf<Operation>()
    private val db = DBHelper(this, null)
    private lateinit var adapter: ArrayAdapter<Operation>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        adapter = object : ArrayAdapter<Operation>(this, R.layout.list_item, operations) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var view = convertView
                if (view == null) {
                    val binding = ListItemBinding.inflate(LayoutInflater.from(context), parent, false)
                    view = binding.root
                    view.tag = binding
                }
                val binding = view.tag as ListItemBinding
                binding.apply {
                    val operation = getItem(position)
                    operation?.let {
                        nameTV.text = String.format(getString(R.string.product_name), operation.name)
                        amountTV.text = String.format(getString(R.string.weight), operation.weight.toString())
                        priceTV.text = String.format(getString(R.string.price), operation.price.toString())
                    }
                    deleteIV.setOnClickListener {
                        deleteItem(position)
                    }
                    editIV.setOnClickListener {
                        updateItem(position)
                    }
                }
                return view
            }
        }
        binding.apply {
            setSupportActionBar(toolbar)
            listLV.adapter = adapter
            lifecycleScope.launch {
                loadAndSetList()
            }
            saveBTN.setOnClickListener {
                if (nameET.text.isBlank() || amountET.text.isBlank() || priceET.text.isBlank()) {
                    makeToast(R.string.fill_all_fields)
                    return@setOnClickListener
                }
                val operation = try {
                    val name = nameET.text.toString()
                    val weight = amountET.text.toString().toInt()
                    val price = priceET.text.toString().toInt()
                    Operation(name = name, weight = weight, price = price)
                } catch (e: NumberFormatException) {
                    makeToast(R.string.enter_valid_amount_and_price)
                    return@setOnClickListener
                }
                lifecycleScope.launch() {
                    withContext(Dispatchers.IO) {
                        db.addOperation(operation)
                    }
                    loadAndSetList()
                }
                nameET.text.clear()
                amountET.text.clear()
                priceET.text.clear()
            }
        }
    }

    private suspend fun loadAndSetList() {
        val list = withContext(Dispatchers.IO) {
            db.getInfo {
                makeToast(R.string.error_loading)
            }
        }
        setList(list)
    }

    private fun setList(list: MutableList<Operation>) {
        operations.clear()
        operations.addAll(list)
        adapter.notifyDataSetChanged()
    }

    private fun deleteItem(position: Int) {
        val operation = operations[position]
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.deleteOperation(operation)
            }
            loadAndSetList()
        }
    }

    private fun updateItem(position: Int) {
        val operation = operations[position]
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogBinding = DialogLayoutBinding.inflate(layoutInflater)
        dialogBinding.apply {
            nameET.setText(operation.name)
            amountET.setText(operation.weight.toString())
            priceET.setText(operation.price.toString())
        }
        dialogBuilder.setView(dialogBinding.root)
            .setTitle(getString(R.string.edit_record))
            .setMessage(getString(R.string.enter_data_below))
            .setPositiveButton (getString(R.string.update)) { _, _ ->
                dialogBinding.apply {
                    if (nameET.text.isBlank() || amountET.text.isBlank() || priceET.text.isBlank()) {
                        makeToast(R.string.fill_all_fields)
                        return@setPositiveButton
                    }
                    try {
                        val name = nameET.text.toString().trim()
                        val weight = amountET.text.toString().trim().toInt()
                        val price = priceET.text.toString().trim().toInt()
                        val updatedOperation = operation.copy(name = name, weight = weight, price = price)
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                db.updateOperation(updatedOperation)
                            }
                            loadAndSetList()
                            makeToast(R.string.record_updated)
                        }
                    } catch (e: NumberFormatException) {
                        makeToast(R.string.enter_valid_amount_and_price)
                        return@setPositiveButton
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_exit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_exit -> {
                moveTaskToBack(true)
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}

fun Activity.makeToast(@StringRes string: Int) {
    Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
}