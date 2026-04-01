package it.bosler.polyphoneme.data.ipa

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Read-only bloom filter loaded from a precomputed .bloom binary file.
 * Uses double hashing (MurmurHash3-based) matching the Python generator.
 */
class BloomFilter private constructor(
    private val k: Int,
    private val m: Int,
    private val bits: ByteArray,
) {
    fun mightContain(word: String): Boolean {
        val key = word.toByteArray(Charsets.UTF_8)
        val h1 = murmurhash3(key, 0)
        val h2 = murmurhash3(key, 0x9747B28C.toInt())
        for (i in 0 until k) {
            var pos = ((h1.toLong() and 0xFFFFFFFFL) + i * (h2.toLong() and 0xFFFFFFFFL)) % m
            if (pos < 0) pos += m
            val p = pos.toInt()
            if (bits[p shr 3].toInt() and (1 shl (p and 7)) == 0) return false
        }
        return true
    }

    companion object {
        fun load(context: Context, assetName: String): BloomFilter {
            val bytes = context.assets.open(assetName).use { it.readBytes() }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val k = buf.int
            val m = buf.int
            val bits = ByteArray(bytes.size - 8)
            System.arraycopy(bytes, 8, bits, 0, bits.size)
            return BloomFilter(k, m, bits)
        }

        private fun murmurhash3(key: ByteArray, seed: Int): Int {
            val c1 = 0xCC9E2D51.toInt()
            val c2 = 0x1B873593
            var h = seed
            val nblocks = key.size / 4

            for (i in 0 until nblocks) {
                var k = (key[i * 4].toInt() and 0xFF) or
                    ((key[i * 4 + 1].toInt() and 0xFF) shl 8) or
                    ((key[i * 4 + 2].toInt() and 0xFF) shl 16) or
                    ((key[i * 4 + 3].toInt() and 0xFF) shl 24)
                k *= c1
                k = Integer.rotateLeft(k, 15)
                k *= c2
                h = h xor k
                h = Integer.rotateLeft(h, 13)
                h = h * 5 + 0xE6546B64.toInt()
            }

            val tail = nblocks * 4
            var k1 = 0
            when (key.size - tail) {
                3 -> {
                    k1 = k1 xor ((key[tail + 2].toInt() and 0xFF) shl 16)
                    k1 = k1 xor ((key[tail + 1].toInt() and 0xFF) shl 8)
                    k1 = k1 xor (key[tail].toInt() and 0xFF)
                    k1 *= c1; k1 = Integer.rotateLeft(k1, 15); k1 *= c2; h = h xor k1
                }
                2 -> {
                    k1 = k1 xor ((key[tail + 1].toInt() and 0xFF) shl 8)
                    k1 = k1 xor (key[tail].toInt() and 0xFF)
                    k1 *= c1; k1 = Integer.rotateLeft(k1, 15); k1 *= c2; h = h xor k1
                }
                1 -> {
                    k1 = k1 xor (key[tail].toInt() and 0xFF)
                    k1 *= c1; k1 = Integer.rotateLeft(k1, 15); k1 *= c2; h = h xor k1
                }
            }

            h = h xor key.size
            h = h xor (h ushr 16)
            h *= 0x85EBCA6B.toInt()
            h = h xor (h ushr 13)
            h *= 0xC2B2AE35.toInt()
            h = h xor (h ushr 16)
            return h
        }
    }
}
