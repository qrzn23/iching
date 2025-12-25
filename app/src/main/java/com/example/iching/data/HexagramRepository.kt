package com.example.iching.data

import android.content.res.AssetManager
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class HexagramRepository(private val assetManager: AssetManager) {
    private var cachedEntries: List<HexagramEntry>? = null
    private var byKeyPrimary: Map<Int, HexagramEntry>? = null
    private var byKingWen: Map<Int, HexagramEntry>? = null

    fun load(): List<HexagramEntry> {
        cachedEntries?.let { return it }
        val jsonText = readAssetText(assetManager, ASSET_PATH)
        val entries = parseJson(jsonText)
        cachedEntries = entries
        byKeyPrimary = entries.associateBy { it.keyPrimary }
        byKingWen = entries.associateBy { it.kingWen }
        return entries
    }

    fun getByKeyPrimary(key: Int): HexagramEntry? {
        if (cachedEntries == null) {
            load()
        }
        return byKeyPrimary?.get(key)
    }

    fun getByKingWen(number: Int): HexagramEntry? {
        if (cachedEntries == null) {
            load()
        }
        return byKingWen?.get(number)
    }

    companion object {
        const val ASSET_PATH = "data/iching.json"

        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun parseJson(text: String): List<HexagramEntry> {
            return try {
                json.decodeFromString<List<HexagramEntry>>(text)
            } catch (e: SerializationException) {
                throw IllegalStateException("Invalid dataset JSON", e)
            }
        }

        private fun readAssetText(assetManager: AssetManager, path: String): String {
            return try {
                assetManager.open(path).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                throw IllegalStateException("Missing dataset: $path", e)
            }
        }
    }
}
