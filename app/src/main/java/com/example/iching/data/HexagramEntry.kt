package com.example.iching.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HexagramEntry(
    @SerialName("key_primary") val keyPrimary: Int,
    @SerialName("king_wen") val kingWen: Int,
    val name: String,
    val description: String = "",
    val judgment: String,
    val image: String,
    val lines: List<String>,
    @SerialName("lines_commentary") val linesCommentary: List<String> = emptyList(),
    val trigrams: Trigrams? = null
)

@Serializable
data class Trigrams(
    val lower: String = "",
    val upper: String = ""
)
