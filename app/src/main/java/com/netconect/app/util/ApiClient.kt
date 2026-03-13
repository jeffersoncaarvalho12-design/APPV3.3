package com.netconect.app.util

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    data class ApiResult(
        val success: Boolean,
        val body: JSONObject? = null,
        val array: JSONArray? = null,
        val message: String = ""
    )

    private fun readAll(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader().use(BufferedReader::readText)
    }

    fun get(url: String, token: String? = null): ApiResult {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", token)

            val code = conn.responseCode
            val text = readAll(if (code in 200..299) conn.inputStream else conn.errorStream)
            if (text.isBlank()) return ApiResult(false, message = "Resposta vazia do servidor")
            val obj = JSONObject(text)
            ApiResult(code in 200..299, body = obj, message = obj.optString("message", ""))
        } catch (e: Exception) {
            ApiResult(false, message = e.message ?: "Erro de conexão")
        }
    }

    fun post(url: String, payload: JSONObject, token: String? = null): ApiResult {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", token)

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val code = conn.responseCode
            val text = readAll(if (code in 200..299) conn.inputStream else conn.errorStream)
            if (text.isBlank()) return ApiResult(false, message = "Resposta vazia do servidor")
            val obj = JSONObject(text)
            ApiResult(code in 200..299, body = obj, message = obj.optString("message", ""))
        } catch (e: Exception) {
            ApiResult(false, message = e.message ?: "Erro de conexão")
        }
    }
}
