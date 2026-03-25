# PolyPhoneme

An Android ebook reader for language learners. Tap any word to see its IPA pronunciation, hear it spoken aloud, and view translations.

## Features

- **EPUB reader** with tap-to-pronounce on every word
- **IPA transcriptions** from offline dictionaries (10 languages: DE, EN, ES, FR, IT, JA, NL, PT, RU, ZH)
- **Homograph disambiguation** using Yarowsky-style context classifiers
- **Text-to-speech** with regional accent support (e.g. Castilian vs Latin American Spanish)
- **Regional pronunciation variants** — data-driven IPA transformations (seseo, yeísmo, etc.)
- **Accent normalization** — handles ebooks with stripped diacritics
- **Word translations** via on-device lookup
- **Bookshelf** with cover art extraction and reading progress

## Build

```sh
./gradlew :composeApp:assembleDebug
```

Releases are automated via conventional commits on push to `main`.
