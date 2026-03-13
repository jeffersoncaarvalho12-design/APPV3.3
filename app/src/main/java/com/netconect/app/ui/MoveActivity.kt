package com.netconect.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import kotlin.concurrent.thread

class MoveActivity : AppCompatActivity() {
    private lateinit var session: SessionManager
    private var currentItemId: Int = 0
    private var capturedPhotoBase64: String = ""

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val imgPreview = findViewById<ImageView>(R.id.imgMovePreview)
            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
            capturedPhotoBase64 = bitmapToBase64(bitmap)
            Toast.makeText(this, "Foto capturada com sucesso", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Foto não capturada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_move)

        session = SessionManager(this)

        val spinner = findViewById<Spinner>(R.id.spinnerMovement)
        val movementLabels = listOf("Entrada", "Saída", "Defeito", "Retornado")
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            movementLabels
        )

        val etBarcode = findViewById<EditText>(R.id.etBarcode)
        val etNote = findViewById<EditText>(R.id.etNote)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val tvItemName = findViewById<TextView>(R.id.tvItemName)
        val tvItemStock = findViewById<TextView>(R.id.tvItemStock)
        val progress = findViewById<ProgressBar>(R.id.progressMove)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val imgPreview = findViewById<ImageView>(R.id.imgMovePreview)

        imgPreview.visibility = View.GONE

        intent.getStringExtra("barcode")?.let {
            etBarcode.setText(it)
            searchItem(it, tvItemName, tvItemStock, progress)
        }

        findViewById<Button>(R.id.btnSearchItem).setOnClickListener {
            val term = etBarcode.text.toString().trim()
            if (term.isBlank()) {
                Toast.makeText(this, "Informe serial, MAC ou nome", Toast.LENGTH_SHORT).show()
            } else {
                searchItem(term, tvItemName, tvItemStock, progress)
            }
        }

        findViewById<Button>(R.id.btnScanMove).setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Escaneie serial ou MAC")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        btnTakePhoto.setOnClickListener {
            cameraLauncher.launch(null)
        }

        findViewById<Button>(R.id.btnSendMove).setOnClickListener {
            if (currentItemId <= 0) {
                Toast.makeText(this, "Busque um item primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val movement = mapMovementLabelToApi(spinner.selectedItem.toString())
            val note = etNote.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (movement == "OUT" && capturedPhotoBase64.isBlank()) {
                Toast.makeText(
                    this,
                    "Na saída é obrigatório tirar foto antes de confirmar",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            sendMovement(
                itemId = currentItemId,
                movement = movement,
                note = note,
                location = location,
                progress = progress,
                tvItemStock = tvItemStock
            )
        }
    }

    private fun mapMovementLabelToApi(label: String): String {
        return when (label) {
            "Entrada" -> "IN_STOCK"
            "Saída" -> "OUT"
            "Defeito" -> "DEFECT"
            "Retornado" -> "RETURNED"
            else -> "OUT"
        }
    }

    private fun searchItem(
        term: String,
        tvItemName: TextView,
        tvItemStock: TextView,
        progress: ProgressBar
    ) {
        progress.visibility = View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/search.php?q=" + URLEncoder.encode(term, "UTF-8"),
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val data = body.opt("data")
                    val item = when (data) {
                        is JSONObject -> data
                        is JSONArray -> if (data.length() > 0) data.optJSONObject(0) else null
                        else -> null
                    }

                    currentItemId = item?.optInt("id", 0) ?: 0

                    val productName = item?.optString("product_name", "Item") ?: "Item"
                    val serial = item?.optString("serial_number", "-") ?: "-"
                    val mac = item?.optString("mac_address", "-") ?: "-"
                    val status = item?.optString("status", "-") ?: "-"
                    val location = item?.optString("location", "-") ?: "-"

                    tvItemName.text = """
                        $productName
                        Serial: $serial
                        MAC: $mac
                    """.trimIndent()

                    tvItemStock.text = """
                        Status atual: ${mapStatusToPt(status)}
                        Local: $location
                    """.trimIndent()
                } else {
                    currentItemId = 0
                    tvItemName.text = "Item não encontrado"
                    tvItemStock.text = "Status atual: -"

                    Toast.makeText(
                        this,
                        body?.optString("message", result.message) ?: result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun mapStatusToPt(status: String): String {
        return when (status) {
            "IN_STOCK" -> "Em estoque"
            "OUT" -> "Saída"
            "DEFECT" -> "Defeito"
            "RETURNED" -> "Retornado"
            else -> status
        }
    }

    private fun sendMovement(
        itemId: Int,
        movement: String,
        note: String,
        location: String,
        progress: ProgressBar,
        tvItemStock: TextView
    ) {
        progress.visibility = View.VISIBLE

        thread {
            val payload = JSONObject().apply {
                put("item_id", itemId)
                put("movement", movement)
                put("technician_id", 0)
                put("note", note)
                put("location", location)
                put("photo_base64", capturedPhotoBase64)
            }

            val result = ApiClient.post(
                session.getBaseUrl() + "/api/move.php",
                payload,
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val data = body.optJSONObject("data")
                    val newStatus = data?.optString("new_status", movement) ?: movement
                    val newLocation = data?.optString("location", "-") ?: "-"

                    tvItemStock.text = """
                        Status atual: ${mapStatusToPt(newStatus)}
                        Local: $newLocation
                    """.trimIndent()

                    Toast.makeText(
                        this,
                        body.optString("message", "Movimentação registrada"),
                        Toast.LENGTH_LONG
                    ).show()

                    capturedPhotoBase64 = ""
                    findViewById<ImageView>(R.id.imgMovePreview).visibility = View.GONE
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

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val etBarcode = findViewById<EditText>(R.id.etBarcode)
            val tvItemName = findViewById<TextView>(R.id.tvItemName)
            val tvItemStock = findViewById<TextView>(R.id.tvItemStock)
            val progress = findViewById<ProgressBar>(R.id.progressMove)

            etBarcode.setText(result.contents)
            searchItem(result.contents, tvItemName, tvItemStock, progress)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
