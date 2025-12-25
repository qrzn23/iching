package com.example.iching.domain

data class CastResult(
    val seed: Long,
    val methodId: String = METHOD_COINS3,
    val lines: IntArray,
    val keyPrimary: Int,
    val keyChanged: Int,
    val changingLines: List<Int>
)

const val METHOD_COINS3 = "coins3_v1"
