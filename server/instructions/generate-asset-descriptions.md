
# Asset Descriptions

## langchain4j

[ ] Das modul world-generate soll die bibliothek langchain4j benutzen.

[ ] Erstelle einen AiModelService der es erlaubt verschiedene modelle zu verwalten.
- Interface LangchainModel wird hier in einer Lazy liste verwaltet. 
- Interface AiModel das AI chats erzeugen kann
- LangchainModel können verschiedene AiModel erzeugen
- Ai-Model-Namen: <LangchainModel>:<AiModel>, z.b. chatgpt:gpt-4
- AiModelService macht ein mapping von "default:<name>" auf ein model name ueber den SettingService.

