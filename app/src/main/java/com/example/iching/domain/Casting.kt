package com.example.iching.domain

fun castCoins3(seed: Long): IntArray {
    val rng = Lcg(seed)
    return IntArray(6) {
        var total = 0
        repeat(3) {
            val coin = if (rng.nextInt(2) == 0) 2 else 3
            total += coin
        }
        total
    }
}

fun changedValue(v: Int): Int {
    return when (v) {
        6 -> 7
        9 -> 8
        else -> v
    }
}

fun applyChanges(lines: IntArray): IntArray {
    return IntArray(lines.size) { index -> changedValue(lines[index]) }
}

fun isYang(v: Int): Int {
    val changed = changedValue(v)
    return if (changed == 7) 1 else 0
}

fun primaryBits(lines: IntArray): IntArray {
    return IntArray(lines.size) { index ->
        val value = lines[index]
        if (value == 7 || value == 9) 1 else 0
    }
}

fun changedBits(lines: IntArray): IntArray {
    return IntArray(lines.size) { index ->
        val value = changedValue(lines[index])
        if (value == 7) 1 else 0
    }
}

fun toKey(bits: IntArray): Int {
    var key = 0
    for (i in bits.indices) {
        key = key or ((bits[i] and 1) shl i)
    }
    return key
}

fun changingLines(lines: IntArray): List<Int> {
    val indices = ArrayList<Int>()
    for (i in lines.indices) {
        val value = lines[i]
        if (value == 6 || value == 9) {
            indices.add(i)
        }
    }
    return indices
}

fun castResult(seed: Long): CastResult {
    val lines = castCoins3(seed)
    val keyPrimary = toKey(primaryBits(lines))
    val keyChanged = toKey(changedBits(lines))
    val moving = changingLines(lines)
    return CastResult(
        seed = seed,
        lines = lines,
        keyPrimary = keyPrimary,
        keyChanged = keyChanged,
        changingLines = moving
    )
}

private class Lcg(seed: Long) {
    private var state: Long = seed

    fun nextInt(bound: Int): Int {
        state = state * 6364136223846793005L + 1442695040888963407L
        val value = (state ushr 33).toInt()
        if (bound <= 0) {
            return 0
        }
        val mod = value % bound
        return if (mod < 0) mod + bound else mod
    }
}
