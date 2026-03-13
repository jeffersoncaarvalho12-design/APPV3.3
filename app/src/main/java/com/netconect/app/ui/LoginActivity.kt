package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.ApiClient
import com.netconect.app.util.SessionManager
import org.json.JSONObject
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)
        if (!session.getToken().isNullOrBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progress = findViewById<ProgressBar>(R.id.progressLogin)
        val tvServer = findViewById<TextView>(R.id.tvServer)
        tvServer.text = session.getBaseUrl()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Informe usuário e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progress.visibility = ProgressBar.VISIBLE
            btnLogin.isEnabled = false

            thread {
                val url = session.getBaseUrl() + "/api/login.php"
                val payload = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }
                val result = ApiClient.post(url, payload)
                runOnUiThread {
                    progress.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true
                    val body = result.body
                    if (result.success && body?.optString("status") == "success") {
                        val user = body.optJSONObject("user")
                        session.saveToken(body.optString("token"))
                        session.saveUsername(user?.optString("username") ?: username)
                        session.saveUserPhotoPath(user?.optString("photo_path", null))
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, body?.optString("message", result.message) ?: result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvServer.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
