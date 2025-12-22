
# Compress Chunk

Um die performance zu erhoehen sollen die storage daten die zu WChunk gehoeren compriiert werden.
- Autoarke WChunkService funktion, ggf durch eine Comprimiert/Decomprimier Stream
- Nach ausen hin bleiben alle bestehenden funktionen unveraendert
- Vermerken ob die daten comprimiert gespeichert sind im WChunk metadata (kompatibility, defaut false fuer bestehende daten)
- Eine neue Funktion gibt den Stream auch Comprimiert zurueck ohne Decomprimier Stream dazwischen.
- Erweitere ChunkData (ChunkDate.ts) um optionale byte[] daten die die comprimierten daten enthalten. (danach im modul generated, 'mvn clean install' ausfuehren)
- in ../client/packages/engine im ChunkService muss geprueft werden ob die comprimierten daten ankommen. wenn ja, diese decomprimieren und anstelle des ChunkData objekts nutzen.
- cx, cz, size, status, i (ItemBlockRef) Daten werden wie bisher uebergeben. Nur blocks, heightData und backdrop sind im storage enthalten.
Ob die daten koprimiert abgelegt werden sollen, kann in application.yaml konfiguriert werden

