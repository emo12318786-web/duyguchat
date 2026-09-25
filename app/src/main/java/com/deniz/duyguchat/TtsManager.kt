package com.deniz.duyguchat

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "DuyguChat-TTS"
    }

    private var tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var hazir = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Türkçe TTS desteklenmiyor")
            } else {
                hazir = true
                tts.setSpeechRate(1.0f)
                tts.setPitch(1.0f)
                Log.i(TAG, "✅ TTS hazır (tr-TR)")
            }
        } else {
            Log.e(TAG, "TTS init başarısız: $status")
        }
    }

    fun konus(metin: String) {
        if (!hazir) {
            Log.w(TAG, "TTS hazır değil, atlanıyor: $metin")
            return
        }
        tts.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "duyguchat-${System.currentTimeMillis()}")
        Log.i(TAG, "🔊 $metin")
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {}
    }
}
