package com.deniz.duyguchat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DuyguChat"
        private const val REQ_PERM = 100
    }

    private lateinit var tts: TtsManager
    private lateinit var stt: SttManager
    private lateinit var statusView: TextView
    private lateinit var chatView: TextView
    private lateinit var speakBtn: Button
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout برنامه‌ای
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.bg_dark))
        }

        val title = TextView(this).apply {
            text = "🎭 DuyguChat"
            textSize = 28f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light))
            setPadding(0, 0, 0, 20)
        }

        statusView = TextView(this).apply {
            text = "Durum: Hazır"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.teal_200))
            setPadding(0, 0, 0, 30)
        }

        speakBtn = Button(this).apply {
            text = "🎤 Konuş"
            textSize = 18f
            setOnClickListener { onSpeakClick() }
        }

        chatView = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light))
            setPadding(0, 30, 0, 0)
        }

        val scroll = ScrollView(this).apply { addView(chatView) }

        root.addView(title)
        root.addView(statusView)
        root.addView(speakBtn)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)

        tts = TtsManager(this)
        stt = SttManager(this)

        checkPerms()
        Log.i(TAG, "MainActivity başladı")
    }

    private fun checkPerms() {
        val perm = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), REQ_PERM)
        }
    }

    private fun appendChat(text: String) {
        runOnUiThread {
            chatView.text = "${chatView.text}\n\n$text"
        }
    }

    private fun setStatus(s: String) {
        runOnUiThread {
            statusView.text = "Durum: $s"
        }
    }

    private fun onSpeakClick() {
        if (isBusy) return
        isBusy = true
        speakBtn.isEnabled = false

        lifecycleScope.launch {
            try {
                setStatus("Dinliyorum...")
                val text = stt.dinle()
                Log.i(TAG, "STT sonuç: $text")

                if (text.isNullOrBlank()) {
                    setStatus("Ses anlaşılamadı")
                    appendChat("⚠️ Seni duyamadım, tekrar dener misin?")
                    tts.konus("Seni duyamadım, tekrar dener misin?")
                    return@launch
                }

                appendChat("👤 Sen: $text")

                // TODO: SER (henüz eklenmedi)
                val emotion = "neutral"

                setStatus("Düşünüyorum...")
                val reply = GemmaClient.sor(text, emotion)
                Log.i(TAG, "Gemma cevap: $reply")

                appendChat("🤖 DuyguChat: $reply")

                setStatus("Konuşuyorum...")
                tts.konus(reply)

                setStatus("Hazır")
            } catch (e: Exception) {
                Log.e(TAG, "Hata: ${e.message}", e)
                setStatus("Hata: ${e.message}")
                appendChat("❌ Hata: ${e.message}")
            } finally {
                isBusy = false
                speakBtn.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        stt.destroy()
    }
}
