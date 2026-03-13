package com.netconect.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import kotlin.concurrent.thread

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val session = SessionManager(this)
        val progress = findViewById<ProgressBar>(R.id.progressHistory)
        val listView = findViewById<ListView>(R.id.listHistory)

        progress.visibility = View.VISIBLE

        thread {
            val result = ApiClient.get(
                session.getBaseUrl() + "/api/history.php",
                session.getToken()
            )

            runOnUiThread {
                progress.visibility = View.GONE
                val body = result.body

                if (result.success && body?.optString("status") == "success") {
                    val arr = body.optJSONArray("data")
                    val lines = mutableListOf<String>()

                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)

                            val line = """
                                ${item.optString("created_at")}
                                ${item.optString("product_name")} • ${item.optString("movement")}
                                Serial: ${item.optString("serial_number")}
                                MAC: ${item.optString("mac_address")}
                                Local: ${item.optString("location")}
                            """.trimIndent()

                            lines.add(line)
                        }
                    }

                    listView.adapter = ArrayAdapter(
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
}
