#!/usr/bin/env python3
"""Generate bloom filter files from IPA dictionary text files.

Each .bloom file is a compact binary that allows instant word-existence checks
without loading the full dictionary. Format:
  - 4 bytes (big-endian): k (number of hash functions)
  - 4 bytes (big-endian): m (number of bits)
  - ceil(m/8) bytes: bit array

Uses double hashing (MurmurHash3-based) for k hash functions.
"""

import math
import struct
import sys
from pathlib import Path


def murmurhash3_32(key: bytes, seed: int = 0) -> int:
    """MurmurHash3 32-bit implementation."""
    c1, c2 = 0xCC9E2D51, 0x1B873593
    h = seed & 0xFFFFFFFF
    length = len(key)
    nblocks = length // 4

    for i in range(nblocks):
        k = int.from_bytes(key[i * 4:(i + 1) * 4], 'little', signed=False)
        k = (k * c1) & 0xFFFFFFFF
        k = ((k << 15) | (k >> 17)) & 0xFFFFFFFF
        k = (k * c2) & 0xFFFFFFFF
        h ^= k
        h = ((h << 13) | (h >> 19)) & 0xFFFFFFFF
        h = (h * 5 + 0xE6546B64) & 0xFFFFFFFF

    tail = key[nblocks * 4:]
    k = 0
    if len(tail) >= 3:
        k ^= tail[2] << 16
    if len(tail) >= 2:
        k ^= tail[1] << 8
    if len(tail) >= 1:
        k ^= tail[0]
        k = (k * c1) & 0xFFFFFFFF
        k = ((k << 15) | (k >> 17)) & 0xFFFFFFFF
        k = (k * c2) & 0xFFFFFFFF
        h ^= k

    h ^= length
    h ^= (h >> 16)
    h = (h * 0x85EBCA6B) & 0xFFFFFFFF
    h ^= (h >> 13)
    h = (h * 0xC2B2AE35) & 0xFFFFFFFF
    h ^= (h >> 16)
    return h


def optimal_params(n: int, p: float = 0.01):
    """Calculate optimal bloom filter parameters."""
    m = int(-n * math.log(p) / (math.log(2) ** 2))
    k = max(1, int((m / n) * math.log(2)))
    return m, k


def generate_bloom(words: list[str], m: int, k: int) -> bytearray:
    """Generate bloom filter bit array."""
    bits = bytearray((m + 7) // 8)
    for word in words:
        key = word.encode('utf-8')
        h1 = murmurhash3_32(key, 0)
        h2 = murmurhash3_32(key, 0x9747B28C)
        for i in range(k):
            pos = (h1 + i * h2) % m
            if pos < 0:
                pos += m
            bits[pos >> 3] |= 1 << (pos & 7)
    return bits


def main():
    assets_dir = Path(__file__).parent.parent / "composeApp" / "src" / "androidMain" / "assets"

    for txt_file in sorted(assets_dir.glob("ipa-*.txt")):
        lang = txt_file.stem.replace("ipa-", "")
        print(f"Processing {lang}...", end=" ", flush=True)

        words = []
        with open(txt_file, "r", encoding="utf-8") as f:
            for line in f:
                tab = line.find("\t")
                if tab > 0:
                    word = line[:tab].lower()
                    # Normalize apostrophes
                    word = word.replace("\u2019", "'").replace("\u2018", "'")
                    words.append(word)

        n = len(words)
        m, k = optimal_params(n, 0.01)
        bits = generate_bloom(words, m, k)

        bloom_file = assets_dir / f"bloom-{lang}.bin"
        with open(bloom_file, "wb") as f:
            f.write(struct.pack(">II", k, m))
            f.write(bits)

        size_kb = len(bits) // 1024
        print(f"{n} words, m={m}, k={k}, size={size_kb}KB")

    print("Done!")


if __name__ == "__main__":
    main()
