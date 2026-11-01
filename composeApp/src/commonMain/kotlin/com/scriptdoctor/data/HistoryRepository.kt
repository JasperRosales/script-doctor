package com.scriptdoctor.data

import com.scriptdoctor.model.DecodeResult
import com.scriptdoctor.platform.KeyValueStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Persists decode history and settings on-device. Images are never stored. */
class HistoryRepository(private val store: KeyValueStore = KeyValueStore()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(DecodeResult.serializer())

    fun load(): List<DecodeResult> =
        store.getString(KEY_HISTORY)?.let { runCatching { json.decodeFromString(listSerializer, it) }.getOrNull() }.orEmpty()

    fun save(items: List<DecodeResult>) {
        store.putString(KEY_HISTORY, json.encodeToString(listSerializer, items.take(MAX_ITEMS)))
    }

    var apiKey: String?
        get() = store.getString(KEY_API_KEY)
        set(value) = store.putString(KEY_API_KEY, value?.trim()?.ifEmpty { null })

    companion object {
        private const val KEY_HISTORY = "history_v1"
        private const val KEY_API_KEY = "anthropic_api_key"
        const val MAX_ITEMS = 50
    }
}
