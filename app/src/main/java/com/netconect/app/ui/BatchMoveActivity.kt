package com.netconect.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.concurrent.thread

class BatchMoveActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private var currentBatchId: Int = 0
    private val techIds = mutableListOf<Int>()
    private val productIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_move)
        session = SessionManager(this)

        val spinnerTechnician = findViewById<Spinner>(R.id.spinnerTechnician)
        val spinnerProduct = findViewById<Spinner>(R.id.spinnerBatchProduct)
        val etBatchQty = findViewById<EditText>(R.id.etBatchQty)
        val etOsNumber = findViewById<EditText>(R.id.etOsNumber)
        val etBatchNote = findViewById<EditText>(R.id.etBatchNote)
        val etBatchSearch = findViewById<EditText>(R.id.etBatchSearch)
        val tvBatchInfo = findViewById<TextView>(R.id.tvBatchInfo)
        val tvBatchSelected = findViewById<TextView>(R.id.tvBatchSelected)
        val listBatchItems = findViewById<ListView>(R.id.listBatchItems)
        val progress = findViewById<ProgressBar>(R.id.progressBatch)

        loadTechnicians(spinnerTechnician, progress)
        loadProducts(spinnerProduct)

        findViewById<Button>(R.id.btnCreateBatch).setOnClickListener {
            if (spinnerTechnician.selectedItemPosition < 0 || techIds.isEmpty()) {
                Toast.makeText(this, "Carregue os técnicos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createBatch(
                techIds[spinnerTechnician.selectedItemPosition],
                etOsNumber.text.toString().trim(),
                etBatchNote.text.toString().trim(),
                tvBatchInfo,
                listBatchItems,
                progress
            )
        }

        findViewById<Button>(R.id.btnScanBatch).setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escaneie serial ou MAC")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        findViewById<Button>(R.id.btnAddBatchItem).setOnClickListener {
            if (currentBatchId <= 0) {
                Toast.makeText(this, "Crie o lote primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val term = etBatchSearch.text.toString().trim()
            if (term.isBlank()) {
                Toast.makeText(this, "Informe serial, MAC ou modelo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            searchAndAdd(term, tvBatchSelected, listBatchItems, progress)
        }

        findViewById<Button>(R.id.btnAddBatchByModel).setOnClickListener {
            if (currentBatchId <= 0) {
                Toast.makeText(this, "Crie o lote primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (spinnerProduct.selectedItemPosition < 0 || productIds.isEmpty()) {
                Toast.makeText(this, "Carregue os produtos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val qty = etBatchQty.text.toString().trim().toIntOrNull() ?: 0
            if (qty <= 0) {
                Toast.makeText(this, "Informe a quantidade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addBatchByModel(productIds[spinnerProduct.selectedItemPosition], qty, listBatchItems, progress)
        }

        findViewById<Button>(R.id.btnCloseBatch).setOnClickListener {
            if (currentBatchId <= 0) {
                Toast.makeText(this, "Crie o lote primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            closeBatch(progress)
        }
    }

    private fun loadTechnicians(spinner: Spinner, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/technicians.php",
                session.getToken()
            )
            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val names = mutableListOf<String>()
                    techIds.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        techIds.add(obj.optInt("id"))
                        names.add(obj.optString("name"))
                    }

                    spinner.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                    )
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

    private fun loadProducts(spinner: Spinner) {
        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/catalog_products.php",
                session.getToken()
            )
            runOnUiThread {
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data") ?: JSONArray()
                    val labels = mutableListOf<String>()
                    productIds.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        productIds.add(obj.optInt("id"))
                        labels.add(obj.optString("label", obj.optString("model", "Produto")))
                    }

                    spinner.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        labels
                    )
                }
            }
        }
    }

    private fun createBatch(
        technicianId: Int,
        osNumber: String,
        note: String,
        tvBatchInfo: TextView,
        list: ListView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE
        thread {
            val payload = JSONObject().apply {
                put("technician_id", technicianId)
                put("os_number", osNumber)
                put("note", note)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/create_batch.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    val data = body.optJSONObject("data")
                    currentBatchId = data?.optInt("batch_id", 0) ?: 0

                    tvBatchInfo.text = """
                        Lote ativo: $currentBatchId
                        Técnico: ${data?.optString("technician_name", "-") ?: "-"}
                        OS: ${data?.optString("os_number", "-") ?: "-"}
                    """.trimIndent()

                    list.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        mutableListOf<String>()
                    )

                    Toast.makeText(
                        this,
                        body.optString("message", "Lote criado"),
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun searchAndAdd(
        term: String,
        tvSelected: TextView,
        list: ListView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE
        thread {
            val search = ApiClient.get(
                session.getBaseUrl() + "/api/search.php?q=" + URLEncoder.encode(term, "UTF-8"),
                session.getToken()
            )

            runOnUiThread {
                val body = search.body
                if (search.success && body?.optString("status") == "success") {
                    val data = body.opt("data")
                    val item = when (data) {
                        is JSONObject -> data
                        is JSONArray -> if (data.length() > 0) data.optJSONObject(0) else null
                        else -> null
                    }

                    val itemId = item?.optInt("id", 0) ?: 0

                    tvSelected.text = """
                        Item selecionado: ${item?.optString("product_name", "-") ?: "-"}
                        Serial: ${item?.optString("serial_number", "-") ?: "-"}
                        MAC: ${item?.optString("mac_address", "-") ?: "-"}
                    """.trimIndent()

                    addBatchItem(itemId, list, progress)
                } else {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this,
                        body?.optString("message", search.message) ?: search.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun addBatchItem(itemId: Int, list: ListView, progress: ProgressBar) {
        val payload = JSONObject().apply {
            put("batch_id", currentBatchId)
            put("item_id", itemId)
            put("note", "Saída em lote pelo app")
            put("photo_path", "")
        }

        thread {
            val result = ApiClient.post(
                session.getBaseUrl() + "/api/add_batch_item.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    Toast.makeText(
                        this,
                        body.optString("message", "Item adicionado"),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadBatchItems(list, progress)
                } else {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun addBatchByModel(productId: Int, qty: Int, list: ListView, progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        thread {
            val payload = JSONObject().apply {
                put("batch_id", currentBatchId)
                put("product_id", productId)
                put("quantity", qty)
                put("note", "Saída por modelo no lote")
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/add_batch_item_by_model.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    Toast.makeText(
                        this,
                        body.optString("message", "Itens adicionados"),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadBatchItems(list, progress)
                } else {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadBatchItems(list: ListView, progress: ProgressBar) {
        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/get_batch.php?batch_id=$currentBatchId",
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    val data = body.optJSONObject("data")
                    val items = data?.optJSONArray("items") ?: JSONArray()
                    val totalItems = data?.optInt("total_items", items.length()) ?: items.length()

                    val lines = mutableListOf<String>()
                    lines.add("Total de itens no lote: $totalItems")

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val line = """
                            ${item.optString("brand", "")} ${item.optString("model", "")}
                            Serial: ${item.optString("serial_number", "-")}
                            MAC: ${item.optString("mac_address", "-")}
                        """.trimIndent()

                        lines.add(line)
                    }

                    list.adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        lines
                    )
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

    private fun closeBatch(progress: ProgressBar) {
        progress.visibility = View.VISIBLE
        thread {
            val payload = JSONObject().apply {
                put("batch_id", currentBatchId)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/close_batch.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body
                if (result.success && body?.optString("status") == "success") {
                    val receiptUrl = session.getBaseUrl() +
                        "/api/receipt_batch.php?batch_id=$currentBatchId&token=" +
                        Uri.encode(session.getToken())

                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(receiptUrl)))
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val etBatchSearch = findViewById<EditText>(R.id.etBatchSearch)
            etBatchSearch.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
