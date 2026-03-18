package it.bosler.polyphoneme.data.ipa

interface AudioPlayer {
    fun speak(text: String, language: String)
    fun stop()
    fun release()
    fun setRegionOverrides(regions: Map<String, String>) {}
}
