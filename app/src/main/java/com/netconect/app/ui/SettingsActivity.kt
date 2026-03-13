package com.netconect.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netconect.app.R
import com.netconect.app.util.SessionManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val session = SessionManager(this)
        val etServer = findViewById<EditText>(R.id.etServerUrl)
        val tvUser = findViewById<TextView>(R.id.tvCurrentUser)
        etServer.setText(session.getBaseUrl())
        tvUser.text = "Usuário: ${session.getUsername() ?: "não autenticado"}"

        findViewById<Button>(R.id.btnSaveServer).setOnClickListener {
            val url = etServer.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Informe a URL do servidor", Toast.LENGTH_SHORT).show()
            } else {
                session.saveBaseUrl(url)
                Toast.makeText(this, "Servidor salvo", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            session.clear()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
