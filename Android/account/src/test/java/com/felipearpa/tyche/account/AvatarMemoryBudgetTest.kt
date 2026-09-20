package com.felipearpa.tyche.account

import coil3.Canvas
import coil3.Image
import coil3.memory.MemoryCache
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Pins what the avatar budget relies on in Coil: a memory cache built from
 * [AVATAR_MEMORY_BUDGET_BYTES], the way `TycheApplication` builds the loader's, keeps its
 * strong references within the budget by dropping the least recently used decoded images.
 */
class AvatarMemoryBudgetTest {

    @Test
    fun `given decoded avatars beyond the 16 MiB budget then the retained bytes stay within it`() {
        val memoryCache = MemoryCache.Builder()
            .maxSizeBytes(AVATAR_MEMORY_BUDGET_BYTES)
            .build()

        repeat(ACCOUNT_COUNT) { index -> memoryCache.write(index = index) }

        memoryCache.maxSize shouldBe 16L * 1024L * 1024L
        memoryCache.size shouldBeLessThanOrEqual AVATAR_MEMORY_BUDGET_BYTES
    }

    @Test
    fun `given decoded avatars beyond the 16 MiB budget then the least recently used are evicted`() {
        // Weak references are off here only so an eviction is observable: with them on, the
        // cache keeps answering for an evicted image as long as anything else still holds it.
        val memoryCache = MemoryCache.Builder()
            .maxSizeBytes(AVATAR_MEMORY_BUDGET_BYTES)
            .weakReferencesEnabled(false)
            .build()

        repeat(ACCOUNT_COUNT) { index -> memoryCache.write(index = index) }

        memoryCache[keyOf(index = 0)].shouldBeNull()
        memoryCache[keyOf(index = ACCOUNT_COUNT - 1)].shouldNotBeNull()
    }
}

// 20 accounts at the 512-pixel source size ask for 20 MiB.
private const val ACCOUNT_COUNT = 20

private fun keyOf(index: Int): MemoryCache.Key =
    MemoryCache.Key(
        avatarMemoryCacheKey(
            accountId = "gambler-$index",
            generation = 0,
            bucket = AVATAR_MAX_PIXEL_SIZE,
        ),
    )

private fun MemoryCache.write(index: Int) {
    this[keyOf(index = index)] = MemoryCache.Value(image = DecodedAvatar(sidePx = AVATAR_MAX_PIXEL_SIZE))
}

/** Costs what an ARGB_8888 bitmap of that side costs, without needing a real bitmap. */
private class DecodedAvatar(sidePx: Int) : Image {
    override val size: Long = sidePx.toLong() * sidePx.toLong() * 4L
    override val width: Int = sidePx
    override val height: Int = sidePx
    override val shareable: Boolean = true

    override fun draw(canvas: Canvas) = Unit
}
