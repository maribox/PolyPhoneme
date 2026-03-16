package it.bosler.polyphoneme

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform