package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class EntryActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private val productIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        session = SessionManager(this)

        val spinnerMode = findViewById<Spinner>(R.id.spinnerEntryMode)
        val spinnerProduct = findViewById<Spinner>(R.id.spinnerEntryProduct)
        val etSerial = findViewById<EditText>(R.id.etEntrySerial)
        val etSerials = findViewById<EditText>(R.id.etEntrySerials)
        val etQty = findViewById<EditText>(R.id.etEntryQty)
        val etLocation = findViewById<EditText>(R.id.etEntryLocation)
        val etNote = findViewById<EditText>(R.id.etEntryNote)
        val progress = findViewById<ProgressBar>(R.id.progressEntry)

        spinnerMode.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Entrada unitária", "Entrada em lote", "Entrada por modelo")
        )

        loadProducts(spinnerProduct, progress)
        updateModeUi(spinnerMode.selectedItemPosition, etSerial, etSerials, etQty)

        spinnerMode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateModeUi(position, etSerial, etSerials, etQty)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnEntryScan).setOnClickListener {
            if (spinnerMode.selectedItemPosition == 2) {
                Toast.makeText(
                    this,
                    "No modo por modelo, use produto + quantidade",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escaneie serial ou MAC")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        findViewById<Button>(R.id.btnEntrySubmit).setOnClickListener {
            if (spinnerProduct.selectedItemPosition < 0 || productIds.isEmpty()) {
                Toast.makeText(this, "Carregue os produtos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val productId = productIds[spinnerProduct.selectedItemPosition]
            val location = etLocation.text.toString().trim()
            val note = etNote.text.toString().trim()

            when (spinnerMode.selectedItemPosition) {
                0 -> submitEntrySingle(
                    productId,
                    etSerial.text.toString().trim(),
                    location,
                    note,
                    progress
                )

                1 -> submitEntryBatch(
                    productId,
                    etSerials.text.toString(),
                    location,
                    note,
                    progress
                )

                else -> submitEntryByModel(
                    productId,
                    etQty.text.toString().trim(),
                    location,
                    note,
                    progress
                )
            }
        }
    }

    private fun updateModeUi(mode: Int, etSerial: EditText, etSerials: EditText, etQty: EditText) {
        etSerial.visibility = if (mode == 0) View.VISIBLE else View.GONE
        etSerials.visibility = if (mode == 1) View.VISIBLE else View.GONE
        etQty.visibility = if (mode == 2) View.VISIBLE else View.GONE
    }

    private fun loadProducts(spinner: Spinner, progress: ProgressBar) {
        progress.visibility = View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/catalog_products.php",
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
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

    private fun submitEntrySingle(
        productId: Int,
        serial: String,
        location: String,
        note: String,
        progress: ProgressBar
    ) {
        if (serial.isBlank()) {
            Toast.makeText(this, "Informe serial ou MAC", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("serial", serial)
                put("location", location)
                put("note", note)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/entry_single.php",
                payload,
                session.getToken()
            )

            handleEntryResponse(result, progress)
        }
    }

    private fun submitEntryBatch(
        productId: Int,
        serials: String,
        location: String,
        note: String,
        progress: ProgressBar
    ) {
        if (serials.trim().isBlank()) {
            Toast.makeText(this, "Informe os seriais/MACs", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("serials", serials)
                put("location", location)
                put("note", note)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/entry_batch.php",
                payload,
                session.getToken()
            )

            handleEntryResponse(result, progress)
        }
    }

    private fun submitEntryByModel(
        productId: Int,
        qtyText: String,
        location: String,
        note: String,
        progress: ProgressBar
    ) {
        val qty = qtyText.toIntOrNull() ?: 0
        if (qty <= 0) {
            Toast.makeText(this, "Informe uma quantidade válida", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("product_id", productId)
                put("quantity", qty)
                put("location", location)
                put("note", note)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/entry_by_model.php",
                payload,
                session.getToken()
            )

            handleEntryResponse(result, progress)
        }
    }

    private fun handleEntryResponse(result: ApiClient.ApiResult, progress: ProgressBar) {
        runOnUiThread {
            progress.visibility = View.GONE
            val body = result.body

            if (result.success && body?.optString("status") == "success") {
                Toast.makeText(
                    this,
                    body.optString("message", "Entrada registrada"),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    body?.optString("message", result.message) ?: result.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val spinnerMode = findViewById<Spinner>(R.id.spinnerEntryMode)

            if (spinnerMode.selectedItemPosition == 0) {
                findViewById<EditText>(R.id.etEntrySerial).setText(result.contents)
            } else {
                val et = findViewById<EditText>(R.id.etEntrySerials)
                val current = et.text.toString()
                et.setText(
                    if (current.isBlank()) {
                        result.contents
                    } else {
                        current + "\n" + result.contents
                    }
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
