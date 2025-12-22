
# Asset Descriptions

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

```