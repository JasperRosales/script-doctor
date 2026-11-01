package com.scriptdoctor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.scriptdoctor.api.ClaudeClient
import com.scriptdoctor.data.HistoryRepository
import com.scriptdoctor.model.DecodeResult
import com.scriptdoctor.model.Transcription
import com.scriptdoctor.platform.nowEpochMs
import com.scriptdoctor.safety.SafetyChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface Screen {
    data object Home : Screen
    data class Result(val result: DecodeResult) : Screen
    data object History : Screen
    data object Settings : Screen
}

class AppState(
    private val scope: CoroutineScope,
    private val repo: HistoryRepository = HistoryRepository(),
    private val client: ClaudeClient = ClaudeClient(ClaudeClient.defaultHttpClient(), { repo.apiKey }),
) {
    var screen: Screen by mutableStateOf(Screen.Home)
        private set
    var image: ByteArray? by mutableStateOf(null)
        private set
    var decoding by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
    var history: List<DecodeResult> by mutableStateOf(repo.load())
        private set
    var hasApiKey by mutableStateOf(!repo.apiKey.isNullOrBlank())
        private set

    private var job: Job? = null

    fun navigate(to: Screen) { screen = to }

    fun back(): Boolean {
        if (screen == Screen.Home) return false
        screen = Screen.Home
        return true
    }

    fun onImage(bytes: ByteArray) {
        image = bytes
        error = null
    }

    fun clearImage() { image = null }

    fun decode() {
        val bytes = image ?: return
        if (decoding) return
        decoding = true
        error = null
        job = scope.launch {
            try {
                val response = client.decode(bytes)
                finish(response.transcription, response.model, isSample = false)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error = e.message ?: "Something went wrong."
            } finally {
                decoding = false
            }
        }
    }

    fun cancel() {
        job?.cancel()
        decoding = false
    }

    fun loadSample() = finish(SampleData.transcription, "sample", isSample = true)

    private fun finish(t: Transcription, model: String, isSample: Boolean) {
        val flags = SafetyChecker.check(t)
        val result = DecodeResult(
            id = Random.nextLong().toULong().toString(16),
            createdAtEpochMs = nowEpochMs(),
            transcription = t,
            flags = flags,
            risk = SafetyChecker.overallRisk(flags),
            model = model,
            isSample = isSample,
        )
        if (!isSample) {
            history = listOf(result) + history
            repo.save(history)
        }
        image = null
        screen = Screen.Result(result)
    }

    fun deleteHistory(id: String) {
        history = history.filterNot { it.id == id }
        repo.save(history)
    }

    fun clearHistory() {
        history = emptyList()
        repo.save(history)
    }

    fun currentApiKey(): String = repo.apiKey.orEmpty()

    fun saveApiKey(key: String) {
        repo.apiKey = key
        hasApiKey = !repo.apiKey.isNullOrBlank()
    }
}
