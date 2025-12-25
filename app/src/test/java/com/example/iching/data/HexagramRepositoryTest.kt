package com.example.iching.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HexagramRepositoryTest {
    @Test
    fun datasetIsValid() {
        val file = findDatasetFile()
        assertTrue("Dataset file missing", file.exists())
        val entries = HexagramRepository.parseJson(file.readText())

        assertEquals(64, entries.size)

        val keyPrimary = entries.map { it.keyPrimary }
        assertEquals(64, keyPrimary.toSet().size)
        assertTrue(keyPrimary.all { it in 0..63 })

        val kingWen = entries.map { it.kingWen }
        assertEquals(64, kingWen.toSet().size)
        assertTrue(kingWen.all { it in 1..64 })

        assertTrue(entries.all { it.lines.size == 6 })
        assertTrue(entries.all { it.linesCommentary.size == 6 })
    }

    private fun findDatasetFile(): File {
        val candidates = listOf(
            File("app/src/main/assets/data/iching.json"),
            File("src/main/assets/data/iching.json")
        )
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }
}
