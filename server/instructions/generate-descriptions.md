
# Generate Descriptions

## langchain4j

[?] Das modul world-generate soll die bibliothek langchain4j benutzen.

[?] Erstelle einen AiModelService der es erlaubt verschiedene modelle zu verwalten.
- Interface LangchainModel wird hier in einer Lazy liste verwaltet. 
- Interface AiChat das AI chats erzeugen kann
- LangchainModel können verschiedene AiModel erzeugen
- Ai-Model-Namen: <LangchainModel>:<AiModel>, z.b. chatgpt:gpt-4
- AiModelService macht ein mapping von "default:<name>" auf ein model name ueber den SettingService.

LangchainModel:
- getName
- createAiChat(name,options): Optional<AiModel>

AiChat:
- getName
- ask(question): String

Options:
- List of AiTools
- Json definition

[?] Erstelle implementierung fuer gemini
- der gemini zugangs key soll von application.yaml geladen werden.

## asset description

[?] Erstelle einen JobExecutor der alle asset infos einer world laed via
SAssetService (nacheinander, nicht gleichzeitig - memory!) und prueft ob
die descrption leer ist.
Ist sie leer wird mit hilfe von gemini eine description erzeugt und gespeichert.
- Nur bei bildern mit einer maximalen byte grenze.
- das wurde schinmal in einem script umgesetzt: ../scripts/generate_asset_info.py
- Wichtig, es gibt ein rate limit, das sollte sinnig im AiChat / LangchainModel fuer dieses model umgesetzt werden.
- Es soll auch ein einzelner asset pfad angegeben werden können, dann wird die description auf jeden fall generiert.
```text
 ✅ Asset Description Generator mit Gemini AI implementiert

  Ich habe einen vollständigen JobExecutor erstellt, der automatisch AI-Descriptions für Assets generiert.

  🎯 AssetDescriptionGeneratorExecutor

  Hauptmerkmale:
  - Sequenzielle Verarbeitung - Lädt Assets nacheinander (memory-freundlich!)
  - Intelligente Filterung:
    - Nur Assets ohne Description
    - Nur Bild-Dateien (.png, .jpg, .jpeg, .gif, .bmp)
    - Nur bis zu konfigurierbare Maximalgröße (default: 5MB)
  - AI-Integration via AiModelService mit flexiblem Model-Mapping
  - Fehlerbehandlung - Robuste Error-Handling für einzelne Assets

  ⏱️ Rate Limiting Implementation

  RateLimiter Interface:
  - waitIfNeeded() - Blockiert Thread bei Rate-Limit
  - recordRequest() - Zeichnet Requests auf
  - Thread-safe via Synchronization

  SimpleRateLimiter:
  - Sliding-Window-Algorithmus
  - Konfigurierbare Requests pro Minute
  - Automatisches Cleanup alter Requests
  - 5 Unit Tests, alle erfolgreich

  Gemini Integration:
  - Rate Limiting: 15 RPM (Gemini Free Tier)
  - Konfigurierbar via langchain4j.gemini.rate-limit
  - Automatisches Warten zwischen Requests

  ⚙️ Konfiguration (application.yml)

  langchain4j:
    gemini:
      api-key: ${GEMINI_API_KEY:}
      rate-limit: 15

  asset:
    description:
      max-size-bytes: 5242880  # 5MB
      ai-model: default:generate

  📝 Job-Parameter

  {
    "worldId": "world-id-here"
  }

  🔄 Ablauf

  1. Job Start mit worldId Parameter
  2. Assets laden via SAssetService.findByWorldId()
  3. Für jedes Asset:
    - Prüfe ob Description existiert ❌
    - Prüfe Dateityp (nur Bilder) 🖼️
    - Prüfe Dateigröße (< 5MB) 📏
    - Lade Asset Content 📥
    - Validiere Image 🔍
    - Rate Limiting (wartet wenn nötig) ⏳
    - Generiere Description mit Gemini 🤖
    - Speichere in AssetMetadata 💾
  4. Result: "Processed X/Y assets: N generated, M skipped, E errors"

  🎨 AI-Prompt

  Generate a concise, single-sentence description (max 100 characters)
  for a game asset file named '{filename}'.
  Focus on what the asset likely represents in a game context.
  Only return the description, no additional text.

  📊 Features

  ✅ Sequenzielle Asset-Verarbeitung (Memory-schonend)
  ✅ Rate Limiting mit Sliding Window
  ✅ Nur Bilder bis 5MB
  ✅ Überspringt existierende Descriptions
  ✅ Robuste Fehlerbehandlung
  ✅ Progress Logging (alle 10 Assets)
  ✅ Detailliertes Result Reporting
  ✅ Konfigurierbare Model-Auswahl
  ✅ Thread-safe Rate Limiter
  ✅ Umfangreiche Tests

  🚀 Verwendung

  Job erstellen:
  WJob job = WJob.builder()
      .worldId("world-123")
      .executorName("asset-description-generator")
      .parameters(Map.of("worldId", "world-123"))
      .build();

  Voraussetzungen:
  - GEMINI_API_KEY Environment Variable setzen
  - World mit Assets muss existieren
  - Assets müssen im SAssetService gespeichert sein
  
 1. Bulk Mode (wie bisher)
  {
    "worldId": "world-123"
  }
  - Verarbeitet alle Assets
  - Überspringt Assets mit existierenden Descriptions
  - Respektiert Größenlimits und Dateitypen

  2. Single Asset Mode (NEU!)
  {
    "worldId": "world-123",
    "assetPath": "textures/items/sword.png"
  }
  - Verarbeitet nur dieses eine Asset
  - Generiert immer neu - auch wenn Description existiert
  - Perfekt zum gezielten Regenerieren
```
