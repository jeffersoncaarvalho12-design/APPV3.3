package com.netconect.app.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class StockProductsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var progress: ProgressBar
    private lateinit var listStock: ListView
    private lateinit var etSearchStock: EditText
    private lateinit var adapter: ProductStockAdapter
    private val items = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_products)

        session = SessionManager(this)
        progress = findViewById(R.id.progressStock)
        listStock = findViewById(R.id.listStock)
        etSearchStock = findViewById(R.id.etSearchStock)

        adapter = ProductStockAdapter()
        listStock.adapter = adapter

        findViewById<Button>(R.id.btnSearchStock).setOnClickListener {
            loadStock(etSearchStock.text.toString().trim())
        }

        loadStock("")
    }

    private fun loadStock(query: String) {
        progress.visibility = View.VISIBLE

        thread {
            val url = if (query.isBlank()) {
                session.getBaseUrl() + "/api/stock_products.php"
            } else {
                session.getBaseUrl() + "/api/stock_products.php?q=" + URLEncoder.encode(query, "UTF-8")
            }

            val result = ApiClient.get(url, session.getToken())

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    items.clear()
                    for (i in 0 until arr.length()) {
                        items.add(arr.getJSONObject(i))
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    inner class ProductStockAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Any = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@StockProductsActivity)
                .inflate(R.layout.item_stock_card, parent, false)

            val img = view.findViewById<ImageView>(R.id.imgCardProduct)
            val tvName = view.findViewById<TextView>(R.id.tvCardProductName)
            val tvQty = view.findViewById<TextView>(R.id.tvCardQty)
            val tvCategory = view.findViewById<TextView>(R.id.tvCardCategory)
            val tvRef = view.findViewById<TextView>(R.id.tvCardRef)

            val item = items[position]
            tvName.text = item.optString("product_name", "Produto")
            tvQty.text = "Disponível: ${item.optInt("available_qty", 0)}"
            tvCategory.text = item.optString("category_name", "Sem categoria")
            tvRef.text = item.optString("ref_code", "")

            img.setImageResource(android.R.drawable.sym_def_app_icon)
            val photoPath = item.optString("photo_path", "")
            if (photoPath.isNotBlank()) {
                val fullUrl = if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
                    photoPath
                } else {
                    session.getBaseUrl() + if (photoPath.startsWith('/')) photoPath else "/$photoPath"
                }

                thread {
                    try {
                        val conn = URL(fullUrl).openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.doInput = true
                        conn.connect()
                        val bmp = BitmapFactory.decodeStream(conn.inputStream)
                        if (bmp != null) {
                            runOnUiThread { img.setImageBitmap(bmp) }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            return view
        }
    }
}
