package com.deniz.duyguchat

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GemmaClient {

    private const val TAG = "DuyguChat-Gemma"
    private const val API_URL = "https://text.pollinations.ai/openai"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sor(kullaniciMetni: String, duygu: String = "neutral"): String = withContext(Dispatchers.IO) {
        try {
            val duyguTalimati = when (duygu.lowercase()) {
                "sad", "üzgün", "uzgun" -> "Kullanıcı üzgün görünüyor. Empatik ve şefkatli cevap ver."
                "happy", "mutlu" -> "Kullanıcı mutlu görünüyor. Neşeli ve pozitif cevap ver."
                "angry", "kızgın", "kizgin" -> "Kullanıcı kızgın görünüyor. Sakin ve anlayışlı cevap ver."
                "fearful", "korkmuş" -> "Kullanıcı korkmuş görünüyor. Güven ver ve sakinleştir."
                else -> "Normal bir tonda cevap ver."
            }

            val json = JSONObject().apply {
                put("model", "openai")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Sen DuyguChat adında, Türkçe konuşan empatik bir asistansın. " +
                                "Kısa, samimi ve doğal cevaplar ver. $duyguTalimati")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", kullaniciMetni)
                    })
                })
                put("temperature", 0.8)
                put("max_tokens", 200)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                Log.d(TAG, "HTTP ${response.code}: $respBody")

                if (!response.isSuccessful) {
                    return@withContext "Üzgünüm, şu an cevap veremiyorum. (HTTP ${response.code})"
                }

                val jsonResp = JSONObject(respBody)
                val choices = jsonResp.optJSONArray("choices")
                val content = choices?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()

                content?.takeIf { it.isNotEmpty() } ?: "Cevap alamadım."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hata: ${e.message}", e)
            "Bağlantı hatası: ${e.message}"
        }
    }
}
