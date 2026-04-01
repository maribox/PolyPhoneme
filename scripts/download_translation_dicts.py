#!/usr/bin/env python3
"""
Downloads bilingual dictionaries and converts them to multi-sense POS-tagged TSV format.

Output format: word<TAB>pos1:translation1[|pos2:translation2|pos3:translation3]
  - POS codes: n (noun), v (verb), adj (adjective), adv (adverb), ? (unknown)
  - Up to 3 senses per word, ordered by importance (noun > verb > adj > adv)
  - This enables context-aware translation: article before word → pick noun sense, etc.

Sources:
  - X→en dicts: kaikki.org (Wiktionary extracts) — excellent coverage + POS tags
  - en→X dicts: FreeDict StarDict — converted to new format with ? POS

Architecture: pivot through English.
  - dict-{lang}-en.txt : foreign language → English (for reading foreign books)
  - dict-en-{lang}.txt : English → native language (for non-English native speakers)

Usage:
  python3 download_translation_dicts.py [--force]
    --force: re-download even if file exists
"""

import urllib.request
import os
import sys
import tarfile
import gzip
import zlib
import struct
import re
import io
import json
from collections import defaultdict

try:
    from wordfreq import word_frequency
except ImportError:
    print("Warning: wordfreq not installed. Run: pip install wordfreq")
    def word_frequency(word, lang):
        return 1e-6

ASSETS_DIR = os.path.join(
    os.path.dirname(__file__),
    "..", "composeApp", "src", "androidMain", "assets"
)

FORCE = "--force" in sys.argv

# ─── kaikki.org source (X→en) ───────────────────────────────────────────────
KAIKKI_LANG_MAP = {
    "fr": "French",
    "de": "German",
    "es": "Spanish",
    "it": "Italian",
    "pt": "Portuguese",
    "nl": "Dutch",
    "ru": "Russian",
}

# ─── FreeDict StarDict source (en→X) ────────────────────────────────────────
FREEDICT_EN_TO = {
    ("en", "de"): "https://download.freedict.org/dictionaries/eng-deu/1.9-fd1/freedict-eng-deu-1.9-fd1.stardict.tar.xz",
    ("en", "fr"): "https://download.freedict.org/dictionaries/eng-fra/0.1.6/freedict-eng-fra-0.1.6.stardict.tar.xz",
    ("en", "es"): "https://download.freedict.org/dictionaries/eng-spa/2025.11.23/freedict-eng-spa-2025.11.23.stardict.tar.xz",
    ("en", "it"): "https://download.freedict.org/dictionaries/eng-ita/2025.11.23/freedict-eng-ita-2025.11.23.stardict.tar.xz",
    ("en", "pt"): "https://download.freedict.org/dictionaries/eng-por/0.3/freedict-eng-por-0.3.stardict.tar.xz",
    ("en", "nl"): "https://download.freedict.org/dictionaries/eng-nld/0.2/freedict-eng-nld-0.2.stardict.tar.xz",
    ("en", "ru"): "https://download.freedict.org/dictionaries/eng-rus/2025.11.23/freedict-eng-rus-2025.11.23.stardict.tar.xz",
}

# ─── Gloss cleaning ──────────────────────────────────────────────────────────
# Grammatical form-of pattern: catches "feminine singular of X", "first-person future of X", etc.
# Matches any sequence of grammatical descriptors (with hyphens/spaces) followed by "of <word>"
_GRAM = (r'(?:first|second|third|feminine|masculine|neuter|plural|singular|past|present|future'
         r'|conditional|subjunctive|indicative|imperative|active|passive|reflexive'
         r'|comparative|superlative|participle|gerund|progressive|perfect|imperfect'
         r'|pluperfect|person|tense|form|number|gender|case|voice)')
FORM_OF_RE = re.compile(
    rf'^{_GRAM}(?:[\s\-]+{_GRAM})*\s+of\b',
    re.IGNORECASE
)
ALT_FORM_RE = re.compile(
    r'^(alternative|archaic|obsolete|dated|dialectal|uncommon|superseded)\s+(?:\S+\s+)*(form|spelling|variant)\s+of\b',
    re.IGNORECASE
)
DERIVED_FORM_RE = re.compile(
    r'\b(inflection|conjugation|clipping|abbreviation|shortening|initialism'
    r'|ellipsis|contraction|diminutive|augmentative|misspelling|eye\s+dialect'
    r'|short\s+for)\s+(of\b|:)',
    re.IGNORECASE
)
# Unicode curly quotes around translation in form-of entries: ("new") or (\u201cnew\u201d)
FORM_OF_QUOTE_RE = re.compile(r'[\(\s]["\u201c]([^"\u201d]{1,30})["\u201d][\)\s]')

LEADING_QUALIFIER_RE = re.compile(r'^\([^)]{1,40}\)\s+')
TRAILING_PARENS_RE   = re.compile(r'\s*\([^)]{1,60}\)$')
# Pure grammatical form descriptions: "second-person singular imperative", "singular imperative", etc.
# Matches glosses that consist entirely of grammatical descriptor words.
PURE_GRAM_RE = re.compile(
    rf'^{_GRAM}(?:[\s\-]+{_GRAM})*$',
    re.IGNORECASE
)

POS_PRIORITY = {"n": 0, "v": 1, "adj": 2, "adv": 3, "prep": 4, "?": 99}

def normalize_pos(pos: str) -> str:
    p = pos.lower()
    if p in ("noun", "name", "num"):                   return "n"
    if p in ("verb", "verb form"):                     return "v"
    if p in ("adj", "adjective"):                      return "adj"
    if p in ("adv", "adverb"):                         return "adv"
    if p in ("prep", "preposition", "particle"):       return "prep"
    return "?"


def clean_gloss(gloss: str) -> str | None:
    """Return a cleaned short translation, or None if it should be skipped."""
    gloss = gloss.strip()
    if not gloss:
        return None

    # Skip pure grammatical form descriptions like "second-person singular imperative", "singular imperative"
    if PURE_GRAM_RE.match(gloss):
        return None

    # Skip form-of / inflection-of entries; try to salvage quoted target word (handles curly quotes too)
    if FORM_OF_RE.match(gloss) or ALT_FORM_RE.match(gloss) or DERIVED_FORM_RE.search(gloss):
        m = FORM_OF_QUOTE_RE.search(gloss)
        return m.group(1).strip() if m else None

    # Strip leading qualifier "(archaic) foo" → "foo"
    gloss = LEADING_QUALIFIER_RE.sub("", gloss)

    # Strip trailing parenthetical qualifier "cat (animal)" → "cat"
    gloss = TRAILING_PARENS_RE.sub("", gloss).strip()

    # Take only the first comma-separated term for brevity
    first = gloss.split(";")[0].split(",")[0].strip()

    # Length sanity check — keep translations short and app-friendly
    if not first or len(first) < 2 or len(first) > 25:
        return None
    if first.startswith("/") or "Usage:" in first:
        return None

    return first


# ─── kaikki.org downloader ───────────────────────────────────────────────────

def iter_jsonl_gz(url: str):
    """Yield decoded JSON objects from a remote .jsonl.gz file, streaming."""
    req = urllib.request.Request(url, headers={"User-Agent": "PolyPhoneme/1.0"})
    resp = urllib.request.urlopen(req, timeout=120)
    d = zlib.decompressobj(zlib.MAX_WBITS | 16)  # gzip mode
    buf = b""
    try:
        while True:
            chunk = resp.read(131072)  # 128 KB
            if not chunk:
                break
            buf += d.decompress(chunk)
            lines = buf.split(b"\n")
            buf = lines[-1]
            for line in lines[:-1]:
                if not line.strip():
                    continue
                try:
                    yield json.loads(line)
                except json.JSONDecodeError:
                    pass
    finally:
        resp.close()
    # Handle any trailing data
    if buf.strip():
        try:
            yield json.loads(buf)
        except json.JSONDecodeError:
            pass


def build_kaikki_dict(src_lang: str, lang_name: str) -> list[tuple[str, str]]:
    """
    Download kaikki.org dictionary for src_lang and return (word, sense_str) pairs
    where sense_str = "pos1:trans1|pos2:trans2|..." (up to 3 senses).
    """
    url = f"https://kaikki.org/dictionary/{lang_name}/kaikki.org-dictionary-{lang_name}.jsonl.gz"
    print(f"  Streaming {url} ...")

    # word → { pos → list of translations }
    word_senses: dict[str, dict[str, list[str]]] = defaultdict(lambda: defaultdict(list))

    count = 0
    for obj in iter_jsonl_gz(url):
        word = obj.get("word", "").strip()
        pos = normalize_pos(obj.get("pos", "?"))

        # Skip multi-word entries, symbols, etc.
        if not word or " " in word or len(word) > 40:
            continue
        if re.search(r"[^\w\-\'\u2019]", word):
            continue

        key = word.lower()

        # For inflected/derived forms: try to salvage quoted translations only
        if obj.get("form_of") or "form-of" in obj.get("tags", []):
            for sense in obj.get("senses", []):
                for gloss in sense.get("glosses", []):
                    # e.g. "feminine singular of nouveau ("new")" → extract "new"
                    m = re.search(r'\("([^"]{1,30})"\)', gloss) or re.search(r"\('([^']{1,30})'\)", gloss)
                    if m:
                        t = m.group(1).strip()
                        if t and len(t) >= 2 and not t.startswith("/"):
                            word_senses[key][pos].append(t)
            continue  # Skip normal sense processing for form-of entries

        for sense in obj.get("senses", []):
            form_of_list = sense.get("form_of")
            if form_of_list or "form-of" in sense.get("tags", []):
                # Extract translation from form_of[].extra (kaikki.org puts it here)
                if isinstance(form_of_list, list):
                    for fo in form_of_list:
                        extra = fo.get("extra", "").strip() if isinstance(fo, dict) else ""
                        if extra and 2 <= len(extra) <= 30 and not extra.startswith("/"):
                            word_senses[key][pos].append(extra)
                # Fallback: extract from quoted part of gloss
                if not word_senses[key][pos]:
                    for gloss in sense.get("glosses", []):
                        t = clean_gloss(gloss)
                        if t:
                            word_senses[key][pos].append(t)
                            break
                continue
            glosses = sense.get("glosses", [])
            if not glosses:
                continue
            translation = clean_gloss(glosses[0])
            if translation:
                word_senses[key][pos].append(translation)
        count += 1
        if count % 100_000 == 0:
            print(f"    ... {count:,} entries processed")

    print(f"  Processed {count:,} total entries, {len(word_senses):,} unique words")

    # Build output: pick best translation per POS, keep top-3 POSes by priority
    # Strategy: use first-sense ordering (Wiktionary lists primary sense first),
    # but skip senses that are clearly definitions rather than translations.
    # For verbs: prefer shorter "to X" forms (real translations) over longer definitions.
    pairs = []
    for word, pos_map in word_senses.items():
        senses = []
        for pos in sorted(pos_map.keys(), key=lambda p: POS_PRIORITY.get(p, 99)):
            translations = [t for t in pos_map[pos] if t]  # skip empty strings
            if not translations:
                continue
            if pos == "v":
                # For verbs: prefer short "to X" translations (1-3 words),
                # then fall back to first sense. Avoid picking "to be" for everything.
                def verb_quality(t: str) -> tuple:
                    words = t.split()
                    is_to_form = t.lower().startswith("to ")
                    word_count = len(words)
                    # Penalize ultra-common auxiliary "to be" — it's almost never the right translation
                    is_to_be = t.lower() in ("to be", "to have", "to do", "to go", "to get", "to make")
                    # Best: short "to X" (2-3 words) that isn't "to be"
                    # Score: (is_real_verb, not_auxiliary, shortness, first_seen_bonus)
                    return (
                        is_to_form and word_count <= 3,  # is it "to X" format?
                        not is_to_be,                     # not a generic auxiliary?
                        -word_count,                       # shorter is better
                    )
                best = max(translations, key=verb_quality)
            else:
                # First valid translation (primary sense in Wiktionary order),
                # but skip entries that look like definitions (>3 words, starts with capital)
                best = None
                for t in translations:
                    words = t.split()
                    # Skip multi-word definitions
                    if len(words) > 3 and words[0][0].isupper():
                        continue
                    # Skip metadata-like entries
                    if any(w in t.lower() for w in ("form of", "variant of", "see ", "cf.")):
                        continue
                    # Prefer single-word translations
                    if len(words) == 1:
                        best = t
                        break
                    if best is None:
                        best = t
                if best is None:
                    best = translations[0]
            senses.append(f"{pos}:{best}")
            if len(senses) >= 3:
                break
        if senses:
            pairs.append((word, "|".join(senses)))

    # Filter to reasonably common words (saves asset space)
    # Use a lower threshold to keep more words — the app's override system
    # handles the most common words anyway, so dict is mainly for less-common words
    pairs = [(w, s) for w, s in pairs
             if word_frequency(w, src_lang) > 1e-8 or len(w) <= 3 or any(c in w for c in "-'")]

    print(f"  Kept {len(pairs):,} words after frequency filter")
    return pairs


# ─── FreeDict StarDict parser ────────────────────────────────────────────────
STRIP_HTML = re.compile(r'<[^>]+>')
POS_WORDS = {
    'noun', 'verb', 'adjective', 'adverb', 'preposition', 'pronoun',
    'conjunction', 'interjection', 'article', 'determiner',
    'male', 'female', 'neuter', 'masculine', 'feminine', 'gender',
    'plural', 'singular', 'invariable', 'transitive', 'intransitive'
}


def score_translation_freedict(t: str, target_lang: str = 'en') -> float:
    words = t.split()
    freq = word_frequency(t.lower(), target_lang)
    if not freq and len(words) == 1:
        freq = word_frequency(words[0].lower(), target_lang)
    freq_score = max(freq * 100000, 0.01)
    word_count_bonus = 3.0 if len(words) == 1 else (1.5 if len(words) == 2 else 0.5)
    noise_penalty = 0
    if any(w.lower() in POS_WORDS for w in words):
        noise_penalty = -5
    if t.startswith('/') or 'Usage:' in t:
        noise_penalty = -10
    return freq_score * word_count_bonus + noise_penalty


def extract_from_freedict_html(html: str, target_lang: str = 'en') -> str:
    clean = re.sub(r'<div[^>]*class=["\']example["\'][^>]*>.*?</div>', '', html, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<a\b[^>]*>.*?</a>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<font[^>]*color=["\']?gray["\']?[^>]*>.*?</font>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<font[^>]*color=["\']?green["\']?[^>]*>.*?</font>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<font[^>]*>.*?</font>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<div[^>]*>[^<]*Usage:[^<]*</div>', '', clean, flags=re.DOTALL | re.IGNORECASE)

    candidates = []

    def add_candidate(text):
        text = text.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&').replace('&nbsp;', ' ')
        text = ' '.join(text.split()).strip()
        if text and len(text) > 1 and len(text) < 50 and not text.startswith('/'):
            candidates.append(text)

    for m in re.finditer(r'<div\s+lang=["\'][a-z]+["\'][^>]*>(.*?)</div>', clean, re.DOTALL | re.IGNORECASE):
        content = m.group(1)
        if '<' not in content:
            add_candidate(content)
    for m in re.finditer(r'<div[^>]*>(.*?)</div>', clean, re.DOTALL):
        content = m.group(1)
        if '<' not in content:
            add_candidate(content)
    for m in re.finditer(r'<li[^>]*>(.*?)</li>', clean, re.DOTALL):
        content = m.group(1)
        if '<ol' not in content:
            add_candidate(STRIP_HTML.sub('', content))

    if candidates:
        return max(candidates, key=lambda t: score_translation_freedict(t, target_lang))

    text = STRIP_HTML.sub(' ', html)
    text = text.replace('&lt;', '<').replace('&gt;', '>').replace('&amp;', '&').replace('&nbsp;', ' ')
    for part in text.split('\n'):
        part = ' '.join(part.split()).strip()
        if not part or part.startswith('/'):
            continue
        words = part.split()
        if all(w.lower().rstrip(',') in POS_WORDS for w in words):
            continue
        while words and words[0].lower().rstrip(',') in POS_WORDS:
            words.pop(0)
        if words:
            return ' '.join(words[:3])
    return ''


def parse_stardict(idx_data: bytes, dict_data: bytes, target_lang: str = 'en') -> list[tuple[str, str]]:
    all_translations: dict[str, list[str]] = defaultdict(list)
    pos = 0
    while pos < len(idx_data):
        end = idx_data.find(b'\x00', pos)
        if end == -1:
            break
        headword = idx_data[pos:end].decode('utf-8', errors='replace')
        pos = end + 1
        if pos + 8 > len(idx_data):
            break
        offset = struct.unpack('>I', idx_data[pos:pos+4])[0]
        size   = struct.unpack('>I', idx_data[pos+4:pos+8])[0]
        pos += 8
        definition = dict_data[offset:offset+size].decode('utf-8', errors='replace')
        translation = extract_from_freedict_html(definition, target_lang)
        key = headword.lower().strip()
        if not key or ' ' in key or len(key) > 30:
            continue
        if re.search(r'[^\w\-\'\u2019]', key):
            continue
        if translation and 2 <= len(translation) < 60:
            all_translations[key].append(translation)

    pairs = []
    for headword, translations in all_translations.items():
        best = max(translations, key=lambda t: score_translation_freedict(t, target_lang))
        # Use "?" POS since FreeDict doesn't provide POS info
        pairs.append((headword, f"?:{best}"))
    return pairs


def process_freedict_archive(tar_bytes: bytes, target_lang: str = 'en') -> list[tuple[str, str]] | None:
    idx_data = None
    dict_data = None
    with tarfile.open(fileobj=io.BytesIO(tar_bytes), mode='r:xz') as tf:
        for member in tf.getmembers():
            name = member.name.lower()
            if name.endswith('.idx.gz'):
                idx_data = gzip.decompress(tf.extractfile(member).read())
            elif name.endswith('.idx') and not name.endswith('.idx.gz'):
                idx_data = tf.extractfile(member).read()
            elif name.endswith('.dict.dz'):
                dict_data = gzip.decompress(tf.extractfile(member).read())
            elif name.endswith('.dict') and not name.endswith('.dict.dz'):
                dict_data = tf.extractfile(member).read()
    if idx_data is None or dict_data is None:
        return None
    return parse_stardict(idx_data, dict_data, target_lang)


def download_freedict(src: str, tgt: str, url: str) -> list[tuple[str, str]] | None:
    print(f"  Downloading FreeDict {url} ...")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "PolyPhoneme/1.0"})
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = resp.read()
    except Exception as e:
        print(f"  Download failed: {e}")
        return None
    print(f"  Downloaded {len(data)//1024}KB, parsing ...")
    return process_freedict_archive(data, target_lang=tgt)


# ─── Main ─────────────────────────────────────────────────────────────────────

def write_dict(pairs: list[tuple[str, str]], out_path: str, label: str):
    with open(out_path, "w", encoding="utf-8") as f:
        for word, senses in pairs:
            if "\t" not in word and "\n" not in word:
                f.write(f"{word}\t{senses}\n")
    print(f"  Saved {len(pairs):,} entries → {label}")


def main():
    os.makedirs(ASSETS_DIR, exist_ok=True)

    # ── X → en (kaikki.org) ───────────────────────────────────────────────────
    for src, lang_name in KAIKKI_LANG_MAP.items():
        out_path = os.path.join(ASSETS_DIR, f"dict-{src}-en.txt")
        label = f"dict-{src}-en.txt"
        if os.path.exists(out_path) and not FORCE:
            print(f"\n{label}: exists ({os.path.getsize(out_path)//1024}KB) — skipping (use --force to regenerate)")
            continue
        print(f"\n{label} (kaikki.org {lang_name}):")
        try:
            pairs = build_kaikki_dict(src, lang_name)
            if pairs:
                write_dict(pairs, out_path, label)
        except Exception as e:
            print(f"  Failed: {e}")

    # ── en → X (FreeDict) ─────────────────────────────────────────────────────
    for (src, tgt), url in FREEDICT_EN_TO.items():
        out_path = os.path.join(ASSETS_DIR, f"dict-{src}-{tgt}.txt")
        label = f"dict-{src}-{tgt}.txt"
        if os.path.exists(out_path) and not FORCE:
            print(f"\n{label}: exists ({os.path.getsize(out_path)//1024}KB) — skipping (use --force to regenerate)")
            continue
        print(f"\n{label} (FreeDict):")
        pairs = download_freedict(src, tgt, url)
        if pairs:
            write_dict(pairs, out_path, label)
        else:
            print(f"  Failed to download/parse")

    print("\nDone.")


if __name__ == "__main__":
    main()
