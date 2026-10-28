package com.tanix.launcher.ai

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.LauncherSettings
import com.tanix.launcher.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Хранилище для API-ключа
object AstraKey {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("astra", Context.MODE_PRIVATE)
    fun get(ctx: Context): String = sp(ctx).getString("key", "") ?: ""
    fun set(ctx: Context, v: String) { sp(ctx).edit().putString("key", v.trim()).apply() }
}

class Astra(
    private val apiKey: () -> String,
    private val settings: () -> LauncherSettings,
    private val apps: () -> List<AppItem>,
    private val openApp: (String) -> Boolean,
    private val model: String = "claude-3-haiku-20240307" // Самая быстрая модель для ТВ
) {
    private var history = JSONArray()
    private val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()

    private val system = "Ты — Астра, голосовой помощник лаунчера Tanix для ТВ. Отвечай по-русски, очень коротко (1-2 предложения). Если просят погоду — вызови get_weather. Если просят открыть приложение — вызови open_app."

    private val tools = JSONArray().apply {
        put(JSONObject().put("name", "get_weather").put("description", "Погода")
            .put("input_schema", JSONObject().put("type", "object").put("properties", JSONObject()
                .put("city", JSONObject().put("type", "string")))))
        put(JSONObject().put("name", "open_app").put("description", "Открыть приложение")
            .put("input_schema", JSONObject().put("type", "object").put("properties", JSONObject()
                .put("name", JSONObject().put("type", "string"))).put("required", JSONArray().put("name"))))
    }

    suspend fun ask(text: String): String = withContext(Dispatchers.IO) {
        if (apiKey().isBlank()) return@withContext "Введите API-ключ Anthropic."
        try {
            if (history.length() > 24) history = JSONArray()
            history.put(JSONObject().put("role", "user").put("content", text))
            for (step in 0 until 5) {
                val resp = call()
                val content = resp.getJSONArray("content")
                history.put(JSONObject().put("role", "assistant").put("content", content))
                if (resp.optString("stop_reason") != "tool_use") return@withContext textOf(content)

                val results = JSONArray()
                for (i in 0 until content.length()) {
                    val b = content.getJSONObject(i)
                    if (b.optString("type") == "tool_use") {
                        val out = runTool(b.getString("name"), b.optJSONObject("input") ?: JSONObject())
                        results.put(JSONObject().put("type", "tool_result").put("tool_use_id", b.getString("id")).put("content", out))
                    }
                }
                history.put(JSONObject().put("role", "user").put("content", results))
            }
            "Не получилось."
        } catch (e: Exception) {
            history = JSONArray()
            "Ошибка: ${e.message}"
        }
    }

    private fun call(): JSONObject {
        val body = JSONObject().put("model", model).put("max_tokens", 512)
            .put("system", system).put("tools", tools).put("messages", history)
        val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey().trim())
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use { r ->
            val raw = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw RuntimeException("Ошибка API: ${r.code}")
            return JSONObject(raw)
        }
    }

    private fun textOf(content: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val b = content.getJSONObject(i)
            if (b.optString("type") == "text") sb.append(b.getString("text"))
        }
        return sb.toString().trim().ifEmpty { "Готово." }
    }

    private suspend fun runTool(name: String, input: JSONObject): String = when (name) {
        "get_weather" -> weather(input.optString("city", ""))
        "open_app" -> openByName(input.optString("name", ""))
        else -> "Ошибка"
    }

    private fun weather(city: String): String {
        val s = settings()
        var cfg = s
        var place = s.city
        if (city.isNotBlank()) {
            val c = Repo.searchCity(city).firstOrNull() ?: return "Город не найден"
            cfg = s.copy(lat = c.lat, lon = c.lon)
            place = c.name
        }
        return Repo.loadWeather(cfg).fold(
            onSuccess = { w -> "В $place сейчас ${w.temp.roundToInt()} градусов, ${describe(w.code)}." },
            onFailure = { "Ошибка погоды" }
        )
    }

    private suspend fun openByName(query: String): String {
        val q = query.trim().lowercase()
        val list = apps()
        val app = list.firstOrNull { it.label.lowercase() == q }
            ?: list.filter { it.label.lowercase().contains(q) }.minByOrNull { it.label.length }
            ?: return "Приложение не найдено."
        val ok = withContext(Dispatchers.Main) { openApp(app.pkg) }
        return if (ok) "Открыто: ${app.label}" else "Не удалось открыть."
    }

    private fun describe(code: Int) = when (code) {
        0 -> "ясно"; 1 -> "малооблачно"; 2 -> "облачно"; 3 -> "пасмурно"
        in 61..67 -> "дождь"; in 71..77 -> "снег"; else -> "облачно"
    }
}

@Composable
fun AstraDialog(vm: HomeViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Если ключа нет, показываем поле для его ввода
    var editKey by remember { mutableStateOf(AstraKey.get(ctx).isBlank()) }
    var keyInput by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("Привет! Я Астра. Спросите про погоду или скажите, какое приложение открыть.") }
    
    // Озвучка (TTS)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(ctx) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale("ru", "RU") }
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    val astra = remember { Astra(apiKey = { AstraKey.get(ctx) }, settings = { vm.s }, apps = { vm.apps }, openApp = { vm.launch(it) }) }

    fun send(q: String) {
        if (q.isBlank() || busy) return
        busy = true; input = ""
        scope.launch { 
            answer = astra.ask(q)
            busy = false 
            tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    // Распознавание голоса
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { send(it) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.widthIn(max = 560.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Астра", style = MaterialTheme.typography.titleLarge)
                Text(if (busy) "Думаю…" else answer, style = MaterialTheme.typography.bodyLarge)

                if (editKey) {
                    // Поле для ввода ключа (появится только при первом запуске)
                    OutlinedTextField(value = keyInput, onValueChange = { keyInput = it }, singleLine = true,
                        label = { Text("API-ключ Anthropic (sk-ant-...)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { if (keyInput.isNotBlank()) { AstraKey.set(ctx, keyInput); editKey = false } }) { Text("Сохранить ключ") }
                } else {
                    // Поле для вопроса
                    OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true,
                        label = { Text("Например: какая погода? / открой YouTube") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send(input) })
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { send(input) }, enabled = !busy) { Text("Спросить") }
                        OutlinedButton(onClick = {
                            try {
                                voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU"))
                            } catch (e: Exception) { answer = "Голосовой ввод недоступен." }
                        }, enabled = !busy) { Text("Голос") }
                    }
                }
            }
        }
    }
}
