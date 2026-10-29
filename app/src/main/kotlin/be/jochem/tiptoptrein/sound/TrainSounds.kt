package be.jochem.tiptoptrein.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { WHISTLE, HORN, BELL, CORRECT, WRONG, POP }

/**
 * All sounds are synthesized at startup and written to small WAV files in the cache, so the app
 * needs no audio assets. Speech uses the device's Dutch text-to-speech voice when available.
 */
object TrainSounds {
  private const val RATE = 22050

  private var pool: SoundPool? = null
  private val ids = mutableMapOf<Sfx, Int>()
  private var tts: TextToSpeech? = null
  private var ttsReady = false

  var muted = false

  fun init(context: Context) {
    if (pool != null) return
    val app = context.applicationContext
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_GAME)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    val soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
    pool = soundPool

    Thread {
      Sfx.entries.forEach { sfx ->
        val file = File(app.cacheDir, "sfx_${sfx.name.lowercase()}.wav")
        writeWav(file, synth(sfx))
        ids[sfx] = soundPool.load(file.path, 1)
      }
    }.start()

    tts = TextToSpeech(app) { status ->
      if (status == TextToSpeech.SUCCESS) {
        // Prefer a Flemish voice; fall back to any Dutch voice.
        val flemish = Locale.forLanguageTag("nl-BE")
        val available = tts?.isLanguageAvailable(flemish) ?: TextToSpeech.LANG_NOT_SUPPORTED
        tts?.language = if (available >= TextToSpeech.LANG_COUNTRY_AVAILABLE) flemish else Locale.forLanguageTag("nl-NL")
        tts?.setSpeechRate(0.9f)
        tts?.setPitch(1.1f)
        ttsReady = true
      }
    }
  }

  fun play(sfx: Sfx) {
    if (muted) return
    val id = ids[sfx] ?: return
    pool?.play(id, 1f, 1f, 1, 0, 1f)
  }

  /** Speaks [text], replacing whatever is being said right now. */
  fun say(text: String) {
    if (muted || !ttsReady) return
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
  }

  /** Speaks [text] after whatever is being said right now. */
  fun sayNext(text: String) {
    if (muted || !ttsReady) return
    tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
  }

  fun stopSpeaking() {
    tts?.stop()
  }

  fun release() {
    tts?.shutdown()
    tts = null
    ttsReady = false
    pool?.release()
    pool = null
    ids.clear()
  }

  // ---- synthesis -------------------------------------------------------------------------

  private fun synth(sfx: Sfx): FloatArray = when (sfx) {
    // Steam whistle: a slightly detuned major chord with some breathy noise.
    Sfx.WHISTLE -> tone(1.1) { t, env ->
      val vib = 1 + 0.004 * sin(2 * PI * 5.5 * t)
      val chord = sin(2 * PI * 587 * vib * t) + 0.8 * sin(2 * PI * 740 * vib * t) +
        0.7 * sin(2 * PI * 880 * vib * t)
      (chord * 0.3 + (Random.nextDouble() - 0.5) * 0.12) * env
    }
    // Two-tone electric train horn with rich harmonics.
    Sfx.HORN -> tone(0.9) { t, env ->
      var s = 0.0
      for (h in 1..5) s += (sin(2 * PI * 311 * h * t) + sin(2 * PI * 370 * h * t)) / (h * 1.6)
      s * 0.22 * env
    }
    // Tram bell: two quick metallic strikes.
    Sfx.BELL -> tone(0.9) { t, _ ->
      val strike = if (t < 0.3) t else t - 0.3
      val d = exp(-strike * 9)
      (sin(2 * PI * 1320 * t) + 0.6 * sin(2 * PI * 2650 * t) + 0.3 * sin(2 * PI * 3960 * t)) * 0.35 * d
    }
    Sfx.CORRECT -> notes(listOf(523.0, 659.0, 784.0, 1047.0), 0.11)
    Sfx.WRONG -> notes(listOf(330.0, 262.0), 0.18)
    Sfx.POP -> tone(0.08) { t, env -> sin(2 * PI * (600 + 3000 * t) * t) * 0.5 * env }
  }

  private inline fun tone(seconds: Double, f: (t: Double, env: Double) -> Double): FloatArray {
    val n = (seconds * RATE).toInt()
    return FloatArray(n) { i ->
      val t = i.toDouble() / RATE
      val env = min(1.0, t / 0.04) * min(1.0, (seconds - t) / 0.12)
      f(t, env).toFloat()
    }
  }

  private fun notes(freqs: List<Double>, each: Double): FloatArray {
    val per = (each * RATE).toInt()
    val out = FloatArray(per * freqs.size + RATE / 4)
    freqs.forEachIndexed { k, freq ->
      for (i in 0 until per + RATE / 4) {
        val idx = k * per + i
        if (idx >= out.size) break
        val t = i.toDouble() / RATE
        val env = min(1.0, t / 0.01) * exp(-t * 7)
        out[idx] += ((sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t)) * 0.3 * env).toFloat()
      }
    }
    return out
  }

  private fun writeWav(file: File, samples: FloatArray) {
    val dataSize = samples.size * 2
    val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
    buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
      .putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
    buf.put("data".toByteArray()).putInt(dataSize)
    samples.forEach { buf.putShort((it.coerceIn(-1f, 1f) * 32000).toInt().toShort()) }
    FileOutputStream(file).use { it.write(buf.array()) }
  }
}
