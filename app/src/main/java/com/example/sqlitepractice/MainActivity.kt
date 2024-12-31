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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sqlitepractice.databinding.ActivityMainBinding
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
                    val amount = amountET.text.toString().toInt()
                    val price = priceET.text.toString().toInt()
                    Operation(name, amount, price)
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