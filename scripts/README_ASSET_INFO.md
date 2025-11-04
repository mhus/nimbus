# Asset Info Generator

Python-Script zur automatischen Generierung von `.info` Metadaten-Dateien für Bild-Assets.

## Features

- **Bildanalyse**: Ermittelt automatisch Breite, Höhe und dominante Farbe
- **KI-Beschreibung**: Nutzt Google Gemini API für intelligente Bildbeschreibungen
- **Batch-Verarbeitung**: Verarbeitet alle Bilder in einem Verzeichnis rekursiv
- **Überspringen vorhandener**: Überspringt bereits vorhandene `.info` Dateien (außer mit `--overwrite`)

## Installation

### Voraussetzungen

Python 3.7 oder höher

### Abhängigkeiten installieren

```bash
pip install Pillow numpy google-generativeai
```

Oder mit der `requirements.txt`:

```bash
# In scripts/ Ordner
pip install -r requirements.txt
```

Die `requirements.txt` enthält:
```
Pillow>=10.0.0
numpy>=1.24.0
google-generativeai>=0.3.0
```

## Verwendung

### Grundlegende Verwendung

```bash
# Von der Projekt-Root aus
python scripts/generate_asset_info.py
```

### Mit Gemini API Key

```bash
# API Key als Argument
python scripts/generate_asset_info.py --api-key "AIza..."

# Oder als Umgebungsvariable
export GOOGLE_API_KEY="AIza..."
python scripts/generate_asset_info.py
```

### Optionen

```bash
# Eigenes Assets-Verzeichnis
python scripts/generate_asset_info.py --assets-dir /path/to/assets

# Vorhandene .info Dateien überschreiben
python scripts/generate_asset_info.py --overwrite

# KI-Beschreibungen überspringen (nur lokale Analyse)
python scripts/generate_asset_info.py --skip-ai

# Kombiniert
python scripts/generate_asset_info.py \
  --assets-dir ./custom/assets \
  --api-key "AIza..." \
  --overwrite
```

## Output Format

Das Script erstellt für jedes Bild eine `.info` Datei im JSON-Format:

**Beispiel: `textures/block/grass.png.info`**

```json
{
  "description": "A vibrant green grass texture with natural variations in color and subtle details, suitable for use as a ground or terrain surface.",
  "width": 512,
  "height": 512,
  "color": "#3a8d2f"
}
```

### Felder

- **description** (string): KI-generierte Beschreibung des Bildinhalts (oder leer wenn `--skip-ai`)
- **width** (number): Bildbreite in Pixeln
- **height** (number): Bildhöhe in Pixeln
- **color** (string): Dominante Farbe als RGB Hex-Code (z.B. `#3a8d2f`)

## Unterstützte Bildformate

- PNG (`.png`)
- JPEG (`.jpg`, `.jpeg`)
- GIF (`.gif`)
- WebP (`.webp`)
- BMP (`.bmp`)

## Gemini API

### API Key erhalten

1. Besuche https://aistudio.google.com/app/apikey
2. Melde dich mit deinem Google Account an
3. Klicke auf "Create API Key"
4. Kopiere den API Key (beginnt mit `AIza...`)

### Kosten

Gemini 1.5 Flash ist **KOSTENLOS** bis zu:
- 15 Anfragen pro Minute
- 1 Million Tokens pro Minute
- 1.500 Anfragen pro Tag

Für kleine 16x16 PNG Bilder:
- ~10-20 tokens pro Bild (sehr effizient)
- **Komplett kostenlos** für normale Nutzung!

Für 753 Bilder: **$0.00** (kostenlos)

**Wichtig**: Gemini Flash hat ein großzügiges kostenloses Kontingent, perfekt für diesen Use Case!

## Beispiele

### Alle Bilder analysieren (ohne KI)

```bash
python scripts/generate_asset_info.py --skip-ai
```

**Output:**
```
Assets directory: ./client/packages/server/files/assets
Overwrite existing: False
AI descriptions: Disabled

Found 15 images

Analyzing: ./client/packages/server/files/assets/textures/block/grass.png
  Dimensions: 512x512
  Color: #3a8d2f
  Skipping AI description (no API key)
  Saved: ./client/packages/server/files/assets/textures/block/grass.png.info

...

Completed!
  Processed: 15
  Skipped: 0
  Total: 15
```

### Mit KI-Beschreibungen

```bash
export GOOGLE_API_KEY="AIza..."
python scripts/generate_asset_info.py
```

**Output:**
```
Analyzing: ./client/packages/server/files/assets/textures/block/grass.png
  Dimensions: 512x512
  Color: #3a8d2f
  Generating AI description...
  Description: A vibrant green grass texture with natural variations...
  Saved: ./client/packages/server/files/assets/textures/block/grass.png.info
```

### Vorhandene überschreiben

```bash
python scripts/generate_asset_info.py --overwrite
```

## Fehlerbehebung

### "Required packages not installed"

```bash
pip install Pillow numpy google-generativeai
```

### "google-generativeai package not installed"

```bash
pip install google-generativeai
```

Das Script funktioniert auch ohne `google-generativeai`, überspringt dann aber die KI-Beschreibungen.

### "No images found"

Prüfe, ob das Assets-Verzeichnis korrekt ist:

```bash
python scripts/generate_asset_info.py --assets-dir /correct/path/to/assets
```

### API-Fehler

- Prüfe, ob API Key korrekt ist
- Prüfe, ob API-Guthaben vorhanden ist
- Prüfe Internetverbindung

## Integration mit Nimbus

Die generierten `.info` Dateien werden automatisch vom Nimbus Asset System erkannt:

1. Server filtert `.info` Dateien aus Asset-Suchen heraus
2. Client kann `.info` Dateien über REST API laden/bearbeiten
3. Im Asset Editor können Benutzer die Metadaten anzeigen und bearbeiten

## Performance

- **Lokale Analyse** (ohne KI): ~0.1-0.5s pro Bild
- **Mit KI-Beschreibung**: ~2-5s pro Bild (abhängig von API-Latenz)

Für große Asset-Bibliotheken:
- 100 Bilder ohne KI: ~30s
- 100 Bilder mit KI: ~5-10 Minuten

**Tipp**: Führe das Script initial mit KI aus, dann nur noch für neue Assets ohne `--overwrite`.

## Lizenz

Teil des Nimbus Projekts.
