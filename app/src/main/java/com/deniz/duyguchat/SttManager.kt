package com.deniz.duyguchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class SttManager(private val context: Context) {

    companion object {
        private const val TAG = "DuyguChat-STT"
    }

    private var recognizer: SpeechRecognizer? = null

    /**
     * Tek seferlik dinleme. Sonuç metin olarak döner (boş olabilir).
     */
    suspend fun dinle(): String? = suspendCancellableCoroutine { cont ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "SpeechRecognizer mevcut değil")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.i(TAG, "🎧 Hazır")
                }
                override fun onBeginningOfSpeech() {
                    Log.i(TAG, "🗣 Konuşma başladı")
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.i(TAG, "⏸ Konuşma bitti")
                }
                override fun onError(error: Int) {
                    Log.w(TAG, "❌ Hata: $error")
                    if (cont.isActive) cont.resume(null)
                }
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull()
                    Log.i(TAG, "✅ Sonuç: $text")
                    if (cont.isActive) cont.resume(text)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    list?.firstOrNull()?.let { Log.d(TAG, "… $it") }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try {
            recognizer?.startListening(intent)
            Log.i(TAG, "▶️ startListening")
        } catch (e: Exception) {
            Log.e(TAG, "startListening hatası: ${e.message}")
            if (cont.isActive) cont.resume(null)
        }

        cont.invokeOnCancellation {
            try { recognizer?.stopListening() } catch (_: Exception) {}
        }
    }

    fun destroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }
}
